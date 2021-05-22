package de.zweistein2.barcoder.qrcode

import de.zweistein2.barcoder.BarcodeEncoder
import de.zweistein2.barcoder.EncodingParameter
import java.util.stream.Collectors
import kotlin.math.*

class QRCodeEncoder : BarcodeEncoder {
    companion object {
        val alphaNumericChars = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                       'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                                       'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                                       'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*',
                                       '+', '-', '.', '/', ':')
        val endBitSequence = "1110110000010001"

        fun Double.roundDownToEvenInt(): Int {
            return (this / 2).toInt() * 2
        }

        fun Byte.toBinaryString(padLeft: Int = 8): String {
            return String.format("%${padLeft}s", Integer.toBinaryString(this.toInt())).replace(' ', '0')
        }

        fun Int.toBinaryString(padLeft: Int = 8): String {
            return String.format("%${padLeft}s", Integer.toBinaryString(this)).replace(' ', '0')
        }

        fun Array<BooleanArray>.toMatrix(): String {
            var output = ""

            output += "\n"
            for(row in this) {
                for(col in row) {
                    output += if(col) { "@" } else { "_" }
                }
                output += "\n"
            }

            return output
        }

        fun Map<String, Array<BooleanArray>>.values(): Array<BooleanArray> {
            return this.getValue("Values")
        }

        fun Map<String, Array<BooleanArray>>.reserved(): Array<BooleanArray> {
            return this.getValue("Reserved")
        }
    }

    override fun encode(content: String, parameters: MutableMap<EncodingParameter, String>): Array<BooleanArray> {
        require(content.isNotBlank()) { "the content shouldn't be empty" }

        val errorCorrectionLevel = parameters[EncodingParameter.ERROR_CORRECTION_LEVEL]?.let { ErrorCorrectionLevel.valueOf(it) } ?: ErrorCorrectionLevel.L
        val charset = parameters[EncodingParameter.CHARSET]?.let { Charset.valueOf(it) } ?: Charset.UTF_8
        val encodingMode = getEncodingModeForContent(content, charset)

        var version: Int = -1

        for(i in 1..40) {
            val capacityInBits = getDataCapacityInBits(getCapacityInBits(i), i, errorCorrectionLevel)
            val capacityForEncodingMode = getDataCapacityForEncodingMode(i, capacityInBits, encodingMode)

            if(content.toList().size < capacityForEncodingMode) {
                version = i
                break
            }
        }

        check(version != -1) { "content is too big for a qr-code" }

        val encodedContentAsBinaryString = encodeContent(content, encodingMode)

        var payloadAsBinaryString = "${encodingMode.modeIndicator.toBinaryString(4)}${content.length.toBinaryString(getCharCountIndicatorSize(version, encodingMode))}${encodedContentAsBinaryString}"
        val dataCapacityInBits = getDataCapacityInBits(getCapacityInBits(version), version, errorCorrectionLevel)

        if(dataCapacityInBits > payloadAsBinaryString.length) {
            if(dataCapacityInBits - payloadAsBinaryString.length > 4) {
                payloadAsBinaryString = payloadAsBinaryString.padEnd(payloadAsBinaryString.length + 4, '0')

                if(payloadAsBinaryString.length % 8 != 0) {
                    payloadAsBinaryString = payloadAsBinaryString.padEnd(payloadAsBinaryString.length + (8 - payloadAsBinaryString.length % 8), '0')
                }

                while (payloadAsBinaryString.length < dataCapacityInBits) {
                    payloadAsBinaryString = "$payloadAsBinaryString$endBitSequence"
                }

                payloadAsBinaryString = payloadAsBinaryString.substring(0, dataCapacityInBits)
            } else {
                payloadAsBinaryString = payloadAsBinaryString.padEnd(dataCapacityInBits, '0')
            }
        }

        val finalPayload = getDataWithInterleavedErrorCorrection(version, errorCorrectionLevel, dataCapacityInBits, payloadAsBinaryString)

        val matrix = MatrixUtil.initiateMatrix(version)
        MatrixUtil.placePayloadInMatrix(matrix, finalPayload)

        val usedMask = putMaskOverMatrix(matrix)
        val formatString = generateFormatString(errorCorrectionLevel, usedMask)
        MatrixUtil.placeFormatInMatrix(matrix, formatString)
        if(version >= 7) {
            val versionString = generateVersionString(version)
            MatrixUtil.placeVersionInMatrix(matrix, versionString)
        }

        return matrix.values()
    }

    private fun generateFormatString(errorCorrectionLevel: ErrorCorrectionLevel, usedMask: MaskPattern): String {
        val generatorPolynomial = 0b10100110111.toBinaryString()
        val bitSequence = 0b101010000010010
        val bits = ((errorCorrectionLevel.bitSequence + usedMask.bitSequence).trimStart('0') + "0000000000").padEnd(11, '0')

        var result: String = (bits.toInt(2) xor generatorPolynomial.padEnd(bits.length, '0').toInt(2)).toBinaryString().trimStart('0')

        while(result.length >= 11) {
            result = (result.toInt(2) xor generatorPolynomial.padEnd(result.length, '0').toInt(2)).toBinaryString().trimStart('0')
        }

        if(result.length < 10) {
            result = result.padStart(10, '0')
        }

        return ((errorCorrectionLevel.bitSequence + usedMask.bitSequence + result).toInt(2) xor bitSequence).toBinaryString(15)
    }

    private fun generateVersionString(version: Int): String {
        val generatorPolynomial = 0b1111100100101.toBinaryString()
        val bits = (version.toBinaryString(6).padEnd(18, '0').trimStart('0')).padEnd(13, '0')

        var result: String = (bits.toInt(2) xor generatorPolynomial.padEnd(bits.length, '0').toInt(2)).toBinaryString().trimStart('0')

        while(result.length >= 13) {
            result = (result.toInt(2) xor generatorPolynomial.padEnd(result.length, '0').toInt(2)).toBinaryString()
        }

        if(result.length < 12) {
            result = result.padStart(12, '0')
        }

        return (version.toBinaryString(6) + result).padStart(18, '0')
    }

    private fun getDataWithInterleavedErrorCorrection(version: Int, errorCorrectionLevel: ErrorCorrectionLevel, dataCapacityInBits: Int, payloadAsBinaryString: String): MutableList<Int> {
        val payloadAsDecimalNumbers = payloadAsBinaryString.chunked(8).stream().map { it.toInt(2) }.collect(Collectors.toList())
        val errorCorrectionCodesPerBlock = getErrorCorrectionCodesPerBlock(version, errorCorrectionLevel)
        val dataBlockCountForGroups = getDataBlockCountForGroups(version, errorCorrectionLevel)

        val totalDataBlocks = if(dataBlockCountForGroups.second != null) { dataBlockCountForGroups.first + dataBlockCountForGroups.second!! } else { dataBlockCountForGroups.first }
        val totalDataCodewords = dataCapacityInBits / 8

        val codewordCountInFirstBlock = floor(totalDataCodewords.toDouble() / totalDataBlocks.toDouble()).toInt()
        val codewordCountInSecondBlock = ceil(totalDataCodewords.toDouble() / totalDataBlocks.toDouble()).toInt()

        val dataCodewordsForGroups = Pair(payloadAsDecimalNumbers.subList(0, codewordCountInFirstBlock * dataBlockCountForGroups.first).toMutableList(), payloadAsDecimalNumbers.subList(codewordCountInFirstBlock * dataBlockCountForGroups.first, payloadAsDecimalNumbers.size).toMutableList())
        val codeBlocks = mutableListOf<Pair<MutableList<Int>, MutableList<Int>>>()

        for(i in 0 until dataBlockCountForGroups.first) {
            val dataCodewordsForBlock = dataCodewordsForGroups.first.subList(i * codewordCountInFirstBlock, (i + 1) * codewordCountInFirstBlock)

            val errorCodewordsForBlock = generateErrorCorrectionCodewordsForBlock(dataCodewordsForBlock, errorCorrectionCodesPerBlock)

            codeBlocks.add(Pair(dataCodewordsForBlock, errorCodewordsForBlock))
        }

        if(dataBlockCountForGroups.second != null) {
            for(i in 0 until dataBlockCountForGroups.second!!) {
                val dataCodewordsForBlock = dataCodewordsForGroups.second.subList(i * codewordCountInSecondBlock, (i + 1) * codewordCountInSecondBlock)

                val errorCodewordsForBlock = generateErrorCorrectionCodewordsForBlock(dataCodewordsForBlock, errorCorrectionCodesPerBlock)

                codeBlocks.add(Pair(dataCodewordsForBlock, errorCodewordsForBlock))
            }
        }

        return interleaveCodewords(codeBlocks)
    }

    private fun interleaveCodewords(codeBlocks: MutableList<Pair<MutableList<Int>, MutableList<Int>>>): MutableList<Int> {
        val size = codeBlocks.stream().map { it.first.size }.max { o1, o2 -> max(o1, o2) }.orElse(0)
        val interleavedDataCodewords = mutableListOf<Int>()
        val interleavedErrorCodewords = mutableListOf<Int>()
        val interleavedCodewords = mutableListOf<Int>()

        for(i in 0 until size) {
            for(j in 0 until codeBlocks.size) {
                if(codeBlocks[j].first.size > i) {
                    interleavedDataCodewords.add(codeBlocks[j].first[i])
                }
            }
        }

        for(i in 0 until codeBlocks[0].second.size) {
            for(j in 0 until codeBlocks.size) {
                interleavedErrorCodewords.add(codeBlocks[j].second[i])
            }
        }

        interleavedCodewords.addAll(interleavedDataCodewords)
        interleavedCodewords.addAll(interleavedErrorCodewords)

        return interleavedCodewords
    }

    private fun generateErrorCorrectionCodewordsForBlock(dataCodewords: MutableList<Int>, errorCorrectionCodewordsToBeGenerated: Int): MutableList<Int> {
        val generatorPolynomial = getGeneratorPolynomial(errorCorrectionCodewordsToBeGenerated).multiplyByMonomial(dataCodewords.size - 1)
        val messagePolynomial = Polynomial(dataCodewords.size - 1, dataCodewords).multiplyByMonomial(errorCorrectionCodewordsToBeGenerated)

        var result = messagePolynomial.divideBy(generatorPolynomial).xorWith(messagePolynomial)

        while(result.coefficients.size != errorCorrectionCodewordsToBeGenerated) {
            result = result.divideBy(generatorPolynomial).xorWith(result)
        }

        return result.coefficients
    }

    private fun getGeneratorPolynomial(errorCorrectionCodewordsToBeGenerated: Int): Polynomial {
        var generatorPolynomial = Polynomial(1, mutableListOf(1, Polynomial.powerGalois(0)))
        for(i in 1 until errorCorrectionCodewordsToBeGenerated) {
            generatorPolynomial = generatorPolynomial.multiplyWith(Polynomial(1, mutableListOf(1, Polynomial.powerGalois(i))))
        }

        return generatorPolynomial
    }

    private fun getCapacityInBits(version: Int): Int {
        val size = version * 4 + 17
        // See https://en.wikipedia.org/wiki/QR_code#/media/File:QR_Code_Structure_Example_3.svg
        val positionBlockCount = 3
        val positionBlockSize = 8 * 8
        val alignmentBlockCount = MatrixUtil.getAlignmentCoordinates(version).size
        val alignmentBlockSize = 5 * 5
        val alignmentBlockCountInTimingRow = MatrixUtil.getAlignmentCoordinates(version).filter { it.first in 0..8 || it.second in 0..8 }.size / 2
        val timingSize = (size - 8 - 8 - alignmentBlockCountInTimingRow * 5) * 2
        val versionSize = if(version >= 7) {36} else {0}
        val formatSize = 31

        return (size * size - (positionBlockCount * positionBlockSize + alignmentBlockCount * alignmentBlockSize + timingSize) - (versionSize + formatSize))
    }

    private fun getErrorCorrectionCodesPerBlock(version: Int, errorCorrectionLevel: ErrorCorrectionLevel): Int {
        // FIXME: No hardcoded table -> try to figure out how they can be calculated

        return when("$version-${errorCorrectionLevel.name}") {
            "1-L" -> { 7 }
			"1-M", "2-L" -> { 10 }
			"1-Q" -> { 13 }
			"3-L" -> { 15 }
			"2-M", "4-H", "6-M" -> { 16 }
			"1-H" -> { 17 }
			"3-Q", "4-M", "5-Q", "6-L", "7-M", "7-Q", "10-L" -> { 18 }
			"4-L", "7-L", "9-Q", "11-L", "14-Q" -> { 20 }
			"2-Q", "3-H", "5-H", "8-M", "8-Q", "9-M", "12-M", "13-M", "13-H", "15-L" -> { 22 }
			"5-M", "6-Q", "8-L", "9-H", "10-Q", "11-H", "12-L", "13-Q", "14-M", "14-H", "15-M", "15-H", "16-L", "16-Q", "22-H" -> { 24 }
			"5-L", "4-Q", "3-M", "7-H", "8-H", "10-M", "12-Q", "13-L", "18-M", "19-M", "19-Q", "19-H", "20-M", "21-M", "25-L" -> { 26 }
            "2-H", "6-H", "11-Q", "12-H", "10-H", "18-Q", "18-H", "19-L", "21-Q", "20-L", "20-H", "21-L", "22-L", "22-M",
                "23-M", "24-M", "25-M", "26-L", "26-M", "26-Q", "27-M", "28-M", "29-M", "30-M", "31-M", "32-M", "33-M", "34-M",
                "35-M", "36-M", "37-M", "38-M", "39-M", "40-M", "17-L", "16-M", "17-M", "17-Q", "17-H" -> { 28 }
			"9-L", "11-M", "14-L", "15-Q", "18-L", "16-H", "20-Q", "21-H", "22-Q", "23-L", "23-Q", "23-H", "24-L", "24-Q",
                "24-H", "25-Q", "25-H", "26-H", "27-L", "27-Q", "27-H", "28-L", "28-Q", "28-H", "29-L", "29-Q", "29-H", "30-L",
                "30-Q", "30-H", "31-L", "31-Q", "31-H", "32-L", "32-Q", "32-H", "33-L", "33-Q", "33-H", "34-L", "34-Q", "34-H",
                "35-L", "35-Q", "35-H", "36-L", "36-Q", "36-H", "37-L", "37-Q", "37-H", "38-L", "38-Q", "38-H", "39-L", "39-Q",
                "39-H", "40-L", "40-Q", "40-H" -> { 30 }
            else -> throw IllegalStateException("the version $version-${errorCorrectionLevel.name} could not be found in the lookup tables")
        }
    }

    private fun getDataBlockCountForGroups(version: Int, errorCorrectionLevel: ErrorCorrectionLevel): Pair<Int, Int?> {
        // FIXME: No hardcoded table -> try to figure out how they can be calculated

        return when("$version-${errorCorrectionLevel.name}") {
            "1-L" -> { Pair(1, null) }
            "1-M" -> { Pair(1, null) }
            "1-Q" -> { Pair(1, null) }
            "1-H" -> { Pair(1, null) }
            "2-L" -> { Pair(1, null) }
            "2-M" -> { Pair(1, null) }
            "2-Q" -> { Pair(1, null) }
            "2-H" -> { Pair(1, null) }
            "3-L" -> { Pair(1, null) }
            "3-M" -> { Pair(1, null) }
            "3-Q" -> { Pair(2, null) }
            "3-H" -> { Pair(2, null) }
            "4-L" -> { Pair(1, null) }
            "4-M" -> { Pair(2, null) }
            "4-Q" -> { Pair(2, null) }
            "4-H" -> { Pair(4, null) }
            "5-L" -> { Pair(1, null) }
            "5-M" -> { Pair(2, null) }
            "5-Q" -> { Pair(2, 2) }
            "5-H" -> { Pair(2, 2) }
            "6-L" -> { Pair(2, null) }
            "6-M" -> { Pair(4, null) }
            "6-Q" -> { Pair(4, null) }
            "6-H" -> { Pair(4, null) }
            "7-L" -> { Pair(2, null) }
            "7-M" -> { Pair(4, null) }
            "7-Q" -> { Pair(2, 4) }
            "7-H" -> { Pair(4, 1) }
            "8-L" -> { Pair(2, null) }
            "8-M" -> { Pair(2, 2) }
            "8-Q" -> { Pair(4, 2) }
            "8-H" -> { Pair(4, 2) }
            "9-L" -> { Pair(2, null) }
            "9-M" -> { Pair(3, 2) }
            "9-Q" -> { Pair(4, 4) }
            "9-H" -> { Pair(4, 4) }
            "10-L" -> { Pair(2, 2) }
            "10-M" -> { Pair(4, 1) }
            "10-Q" -> { Pair(6, 2) }
            "10-H" -> { Pair(6, 2) }
            "11-L" -> { Pair(4, null) }
            "11-M" -> { Pair(1, 4) }
            "11-Q" -> { Pair(4, 4) }
            "11-H" -> { Pair(3, 8) }
            "12-L" -> { Pair(2, 2) }
            "12-M" -> { Pair(6, 2) }
            "12-Q" -> { Pair(4, 6) }
            "12-H" -> { Pair(7, 4) }
            "13-L" -> { Pair(4, null) }
            "13-M" -> { Pair(8, 1) }
            "13-Q" -> { Pair(8, 4) }
            "13-H" -> { Pair(12, 4) }
            "14-L" -> { Pair(3, 1) }
            "14-M" -> { Pair(4, 5) }
            "14-Q" -> { Pair(11, 5) }
            "14-H" -> { Pair(11, 5) }
            "15-L" -> { Pair(5, 1) }
            "15-M" -> { Pair(5, 5) }
            "15-Q" -> { Pair(5, 7) }
            "15-H" -> { Pair(11, 7) }
            "16-L" -> { Pair(5, 1) }
            "16-M" -> { Pair(7, 3) }
            "16-Q" -> { Pair(15, 2) }
            "16-H" -> { Pair(3, 13) }
            "17-L" -> { Pair(1, 5) }
            "17-M" -> { Pair(10, 1) }
            "17-Q" -> { Pair(1, 15) }
            "17-H" -> { Pair(2, 17) }
            "18-L" -> { Pair(5, 1) }
            "18-M" -> { Pair(9, 4) }
            "18-Q" -> { Pair(17, 1) }
            "18-H" -> { Pair(2, 19) }
            "19-L" -> { Pair(3, 4) }
            "19-M" -> { Pair(3, 11) }
            "19-Q" -> { Pair(17, 4) }
            "19-H" -> { Pair(9, 16) }
            "20-L" -> { Pair(3, 5) }
            "20-M" -> { Pair(3, 13) }
            "20-Q" -> { Pair(15, 5) }
            "20-H" -> { Pair(15, 10) }
            "21-L" -> { Pair(4, 4) }
            "21-M" -> { Pair(17, null) }
            "21-Q" -> { Pair(17, 6) }
            "21-H" -> { Pair(19, 6) }
            "22-L" -> { Pair(2, 7) }
            "22-M" -> { Pair(17, null) }
            "22-Q" -> { Pair(7, 16) }
            "22-H" -> { Pair(34, null) }
            "23-L" -> { Pair(4, 5) }
            "23-M" -> { Pair(4, 14) }
            "23-Q" -> { Pair(11, 14) }
            "23-H" -> { Pair(16, 14) }
            "24-L" -> { Pair(6, 4) }
            "24-M" -> { Pair(6, 14) }
            "24-Q" -> { Pair(11, 16) }
            "24-H" -> { Pair(30, 2) }
            "25-L" -> { Pair(8, 4) }
            "25-M" -> { Pair(8, 13) }
            "25-Q" -> { Pair(7, 22) }
            "25-H" -> { Pair(22, 13) }
            "26-L" -> { Pair(10, 2) }
            "26-M" -> { Pair(19, 4) }
            "26-Q" -> { Pair(28, 6) }
            "26-H" -> { Pair(33, 4) }
            "27-L" -> { Pair(8, 4) }
            "27-M" -> { Pair(22, 3) }
            "27-Q" -> { Pair(8, 26) }
            "27-H" -> { Pair(12, 28) }
            "28-L" -> { Pair(3, 10) }
            "28-M" -> { Pair(3, 23) }
            "28-Q" -> { Pair(4, 31) }
            "28-H" -> { Pair(11, 31) }
            "29-L" -> { Pair(7, 7) }
            "29-M" -> { Pair(21, 7) }
            "29-Q" -> { Pair(1, 37) }
            "29-H" -> { Pair(19, 26) }
            "30-L" -> { Pair(5, 10) }
            "30-M" -> { Pair(19, 10) }
            "30-Q" -> { Pair(15, 25) }
            "30-H" -> { Pair(23, 25) }
            "31-L" -> { Pair(13, 3) }
            "31-M" -> { Pair(2, 29) }
            "31-Q" -> { Pair(42, 1) }
            "31-H" -> { Pair(23, 28) }
            "32-L" -> { Pair(17, null) }
            "32-M" -> { Pair(10, 23) }
            "32-Q" -> { Pair(10, 35) }
            "32-H" -> { Pair(19, 35) }
            "33-L" -> { Pair(17, 1) }
            "33-M" -> { Pair(14, 21) }
            "33-Q" -> { Pair(29, 19) }
            "33-H" -> { Pair(11, 46) }
            "34-L" -> { Pair(13, 6) }
            "34-M" -> { Pair(14, 23) }
            "34-Q" -> { Pair(44, 7) }
            "34-H" -> { Pair(59, 1) }
            "35-L" -> { Pair(12, 7) }
            "35-M" -> { Pair(12, 26) }
            "35-Q" -> { Pair(39, 14) }
            "35-H" -> { Pair(22, 41) }
            "36-L" -> { Pair(6, 14) }
            "36-M" -> { Pair(6, 34) }
            "36-Q" -> { Pair(46, 10) }
            "36-H" -> { Pair(2, 64) }
            "37-L" -> { Pair(17, 4) }
            "37-M" -> { Pair(29, 14) }
            "37-Q" -> { Pair(49, 10) }
            "37-H" -> { Pair(24, 46) }
            "38-L" -> { Pair(4, 18) }
            "38-M" -> { Pair(13, 32) }
            "38-Q" -> { Pair(48, 14) }
            "38-H" -> { Pair(42, 32) }
            "39-L" -> { Pair(20, 4) }
            "39-M" -> { Pair(40, 7) }
            "39-Q" -> { Pair(43, 22) }
            "39-H" -> { Pair(10, 67) }
            "40-L" -> { Pair(19, 6) }
            "40-M" -> { Pair(18, 31) }
            "40-Q" -> { Pair(34, 34) }
            "40-H" -> { Pair(20, 61) }
            else -> throw IllegalStateException("the version $version-${errorCorrectionLevel.name} could not be found in the lookup tables")
        }
    }

    private fun getDataCapacityInBits(capacityInBits: Int, version: Int, errorCorrectionLevel: ErrorCorrectionLevel): Int {
        val remainderBits = getRemainderBits(version)
        val errorCorrectionCodesPerBlock = getErrorCorrectionCodesPerBlock(version, errorCorrectionLevel)
        val dataBlockCountForGroups = getDataBlockCountForGroups(version, errorCorrectionLevel)
        val blockOne = dataBlockCountForGroups.first
        val blockTwo = dataBlockCountForGroups.second ?: 0
        val errorCorrectionBits = errorCorrectionCodesPerBlock * (blockOne + blockTwo) * 8 + remainderBits

        return capacityInBits - errorCorrectionBits
    }

    private fun getDataCapacityForEncodingMode(version: Int, dataCapacityInBits: Int, encodingMode: EncodingMode): Int {
        // capacityInBits - 4 Bits (to select encoding mode) - Character Count Indicator / Bits per X Digits * X Digits = Charcount
        return ((dataCapacityInBits - 4.0 - getCharCountIndicatorSize(version, encodingMode)) / encodingMode.bitsPerDigits * encodingMode.digits).toInt()
    }

    private fun getCharCountIndicatorSize(version: Int, encodingMode: EncodingMode): Int {
        // See https://en.wikipedia.org/wiki/QR_code#Encoding
        return when(version) {
            in 1..9 -> { return when(encodingMode) {
                EncodingMode.NUMERIC -> 10
                EncodingMode.ALPHANUMERIC -> 9
                EncodingMode.BINARY -> 8
                EncodingMode.KANJI -> 8
            }}
            in 10..26 -> { return when(encodingMode) {
                EncodingMode.NUMERIC -> 12
                EncodingMode.ALPHANUMERIC -> 11
                EncodingMode.BINARY -> 16
                EncodingMode.KANJI -> 10
            }}
            in 27..40 -> { return when(encodingMode) {
                EncodingMode.NUMERIC -> 14
                EncodingMode.ALPHANUMERIC -> 13
                EncodingMode.BINARY -> 16
                EncodingMode.KANJI -> 12
            }}
            else -> -1
        }
    }

    private fun getRemainderBits(version: Int): Int {
        // https://www.thonky.com/qr-code-tutorial/structure-final-message
        return when(version) {
            in 2..6 -> { 7 }
            in 14..20 -> { 3 }
            in 21..27 -> { 4 }
            in 28..34 -> { 3 }
            else -> 0
        }
    }

    private fun getEncodingModeForContent(content: String, charset: Charset): EncodingMode {
        return when {
            content.matches(Regex("^\\d+$")) -> { EncodingMode.NUMERIC }
            content.chars().allMatch { alphaNumericChars.contains(it.toChar()) } -> { EncodingMode.ALPHANUMERIC }
            charset == Charset.SHIFT_JIS -> { EncodingMode.KANJI }
            else -> { EncodingMode.BINARY }
        }
    }

    private fun encodeContent(content: String, encodingMode: EncodingMode): String {
        var output = ""

        return when(encodingMode) {
            EncodingMode.NUMERIC -> {
                val numberChunks = content.chunked(3)

                for(numbers in numberChunks) {
                    numbers.trimStart('0')

                    output += when(numbers.length) {
                        1 -> { Integer.parseInt(numbers).toBinaryString(4) }
                        2 -> { Integer.parseInt(numbers).toBinaryString(7) }
                        3 -> { Integer.parseInt(numbers).toBinaryString(10) }
                        else -> ""
                    }
                }

                output
            }
            EncodingMode.ALPHANUMERIC -> {
                val charPairs = content.chunked(2)

                for(charPair in charPairs) {
                    if(charPair.length == 2) {
                        output += (alphaNumericChars.indexOf(charPair[0]) * 45 + alphaNumericChars.indexOf(charPair[1])).toBinaryString(11)
                    } else if(charPair.length == 1) {
                        output += alphaNumericChars.indexOf(charPair[0]).toBinaryString(6)
                    }
                }

                output
            }
            EncodingMode.BINARY -> {
                output += content.toCharArray().map { it.code.toBinaryString(8) }.reduce { s1, s2 -> s1 + s2 }

                output
            }
            EncodingMode.KANJI -> {
                val charsAsByte = content.toByteArray(java.nio.charset.Charset.forName("SHIFT_JIS")).map { it.toInt() and 0xFF }
                val kanjiBytes = mutableListOf<Int>()

                val tempList = charsAsByte.chunked(2)
                for(temp in tempList) {
                    kanjiBytes.add((temp[0].toBinaryString(8) + temp[1].toBinaryString(8)).toInt(2))
                }

                for(kanjiByte in kanjiBytes) {
                    if(kanjiByte in 0x8140..0x9FFC) {
                        val fixedKanjiByte = (kanjiByte - 0x8140).toBinaryString(16).chunked(8)

                        output += (fixedKanjiByte[0].toInt(2) * 0xC0 + fixedKanjiByte[1].toInt(2)).toBinaryString(13)
                    } else if(kanjiByte in 0xE040..0xEBBF) {
                        val fixedKanjiByte = (kanjiByte - 0xC140).toBinaryString(16).chunked(8)

                        output += (fixedKanjiByte[0].toInt(2) * 0xC0 + fixedKanjiByte[1].toInt(2)).toBinaryString(13)
                    }
                }

                output
            }
        }
    }

    private fun putMaskOverMatrix(matrix: Map<String, Array<BooleanArray>>): MaskPattern {
        val values = matrix.values()
        val reserved = matrix.reserved()

        var lowestPenalty = Int.MAX_VALUE
        var mask = MaskPattern.PATTERN_1

        for(maskPattern in MaskPattern.values()) {
            val penalty = MaskUtil.determineMaskPenalty(values, reserved, maskPattern)

            if(lowestPenalty > penalty) {
                lowestPenalty = penalty
                mask = maskPattern
            }
        }

        for ((rowIndex, row) in values.withIndex()) {
            for (colIndex in row.indices) {
                if (!reserved[rowIndex][colIndex]) {
                    values[rowIndex][colIndex] = values[rowIndex][colIndex] xor mask.formula.invoke(rowIndex, colIndex)
                }
            }
        }

        return mask
    }
}