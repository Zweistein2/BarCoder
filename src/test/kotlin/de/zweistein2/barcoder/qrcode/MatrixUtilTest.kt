package de.zweistein2.barcoder.qrcode

import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.reserved
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.toMatrix
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.values
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MatrixUtilTest {

    @Test
    fun initiateMatrixTest() {
        val matrixVersion1 = MatrixUtil.initiateMatrix(1)
        val matrixVersion24 = MatrixUtil.initiateMatrix(24)
        val matrixVersion40 = MatrixUtil.initiateMatrix(40)

        assertEquals(21, matrixVersion1.values().size)
        assertEquals(113, matrixVersion24.values().size)
        assertEquals(177, matrixVersion40.values().size)
    }

    @Test
    fun getAlignmentCoordinatesTest() {
        val alignmentCoordinatesVersion1 = MatrixUtil.getAlignmentCoordinates(1)
        val alignmentCoordinatesVersion24 = MatrixUtil.getAlignmentCoordinates(24)
        val alignmentCoordinatesVersion40 = MatrixUtil.getAlignmentCoordinates(40)

        assertEquals(0, alignmentCoordinatesVersion1.size)

        assertEquals(22, alignmentCoordinatesVersion24.size)
        assertEquals(Pair(28, 28), alignmentCoordinatesVersion24[4])
        assertEquals(Pair(80, 106), alignmentCoordinatesVersion24[17])

        assertEquals(46, alignmentCoordinatesVersion40.size)
        assertEquals(Pair(6, 142), alignmentCoordinatesVersion40[4])
        assertEquals(Pair(58, 142), alignmentCoordinatesVersion40[17])
        assertEquals(Pair(114, 86), alignmentCoordinatesVersion40[29])
        assertEquals(Pair(142, 30), alignmentCoordinatesVersion40[34])
    }

    @Test
    fun placeStuffInMatrixTest() {
        val matrix = MatrixUtil.initiateMatrix(7)

        // 7-Q "THIS IS SOME TEXT FOR TESTING PURPOSES ONLY. FOR THE TEST WE NEED A VERSION 7 QRCODE SO WE HAVE TO ADD TEXT."
        val finalPayload = mutableListOf(35, 87, 54, 154, 74, 169, 101, 115, 66, 212, 249, 122, 42, 142, 12, 34, 194, 134,
            104, 147, 73, 109, 46, 0, 217, 208, 152, 60, 90, 236, 154, 150, 226, 214, 236, 17, 136, 130, 158, 6, 202, 236,
            80, 189, 113, 109, 120, 17, 71, 36, 97, 180, 84, 236, 217, 28, 121, 40, 213, 17, 156, 216, 197, 94, 49, 236, 84,
            138, 73, 87, 203, 17, 189, 138, 83, 229, 201, 236, 61, 75, 219, 106, 91, 17, 217, 52, 91, 53, 105, 238, 112, 60,
            188, 150, 224, 104, 111, 19, 156, 77, 121, 178, 73, 210, 75, 195, 27, 109, 175, 157, 96, 91, 28, 29, 29, 190, 177,
            91, 97, 119, 11, 5, 93, 34, 102, 81, 146, 166, 242, 236, 111, 48, 158, 92, 158, 19, 124, 107, 176, 141, 243, 191,
            93, 28, 92, 119, 227, 1, 211, 65, 168, 108, 74, 86, 13, 89, 243, 139, 28, 67, 6, 205, 63, 237, 190, 210, 109, 113,
            19, 117, 226, 104, 149, 236, 81, 116, 94, 246, 221, 76, 249, 136, 122, 90, 182, 81, 64, 135, 11, 227, 127, 52)
        val formatString = "010111011011010"
        val versionString = "000111110010010100"
        val mask = MaskPattern.PATTERN_7

        val values = matrix.values()
        val reserved = matrix.reserved()

        MatrixUtil.placePayloadInMatrix(matrix, finalPayload)

        // Put Mask over Payload
        for ((rowIndex, row) in values.withIndex()) {
            for (colIndex in row.indices) {
                if (!reserved[rowIndex][colIndex]) {
                    values[rowIndex][colIndex] = values[rowIndex][colIndex] xor mask.formula.invoke(rowIndex, colIndex)
                }
            }
        }

        MatrixUtil.placeFormatInMatrix(matrix, formatString)
        MatrixUtil.placeVersionInMatrix(matrix, versionString)

        val finalMatrix = "\n" +
                "@@@@@@@___@___@____@_@__@@__@___@@__@_@@@@@@@\n" +
                "@_____@_@___@____@@_@@@____@__@@___@__@_____@\n" +
                "@_@@@_@____@__@@_____@@@_@_@_@___@_@__@_@@@_@\n" +
                "@_@@@_@_@@____@_@_@@@__@@@@__@@_@@_@@_@_@@@_@\n" +
                "@_@@@_@_@_@_@@_@__@_@@@@@_@@@@_@@_@@@_@_@@@_@\n" +
                "@_____@__@_@@_@@__@_@___@@_@@@@_______@_____@\n" +
                "@@@@@@@_@_@_@_@_@_@_@_@_@_@_@_@_@_@_@_@@@@@@@\n" +
                "________@_@_@___@@__@___@__@@@__@@@_@________\n" +
                "_@_@@@@_@__@__@__@@_@@@@@__@__@__@@__@@_@@_@_\n" +
                "_@@__@_@@@@_@@___@___@@@@__@@@@@@__@___@@@___\n" +
                "__@_@@@@_@@@@@@@@@@__@___@@_@_@@_@@_@@@_@_@_@\n" +
                "_@@@_@__@@__@@_@@@@@@__@@__@__@__@_@_@@_@_@__\n" +
                "____@@@_@__@@__@@@____@_@__@__@@_@_@__@@@@@_@\n" +
                "__@@@@____@@__@@@@_@_@@_@_@__@_@_@@_@@@__@___\n" +
                "@@_@@_@_@__@@@____@___@@_@__@__@_@@_@@_@@@_@_\n" +
                "_@@__@___@@_@@@@@_@@__@@@@@@@_@@@@@________@_\n" +
                "____@_@@_@___@__@_@@@@@@_@@_@______@@@@_@_@@@\n" +
                "@_@_@__@___@____@_@__@@___@@_@@@@_@@__@___@@_\n" +
                "@@@@_@@_@_@@@_@_@_@_@@@@@__@@_@_@@@@@@@@@___@\n" +
                "@@@@_@_@@_@___@__@@___@@@@@__@_@@_@__@@@___@_\n" +
                "@@_@@@@@@__@__@@_@__@@@@@_@_____@__@@@@@@____\n" +
                "@_@_@___@@@_@_@@_@@_@___@@@__@_@@@__@___@_@@@\n" +
                "@_@@@_@_@____@@__@@_@_@_@@@_@@@_@__@@_@_@@___\n" +
                "@_@_@___@@___@@__@__@___@@_@@_@@_@@@@___@@__@\n" +
                "@@_@@@@@@@@_@_@@@@@@@@@@@@_@__@@____@@@@@@_@_\n" +
                "@@@_____@@_____@@__@_@@_@@___@@_@___@__@@__@_\n" +
                "@@@@@@@_@@@@@_@_@@@_@__@______@_@_@_@_@@__@@@\n" +
                "@___@__@@@@@@__@@___@_@@_@_@@___@@@___@@@@__@\n" +
                "@_@_@@@@@@__@_@@@_@__@_@_____@@@_@@_@@@__@_@@\n" +
                "@_@______@___@@@_@_@_@__@@@__@__@@___@___@@_@\n" +
                "@@__@_@_@_@@@@@_@_@__@@@@@___@____@_@@_@@@@@_\n" +
                "@@@@_@__@@@_@@@_@@@@_@____@@@@@@@@_@_@_@_@__@\n" +
                "@@____@@__@__@_@@_@@_@____@@@_@_______@___@@@\n" +
                "@_@@___@_@@@@@@_@@@_@@_@@_@@@__@@_______@_@__\n" +
                "____@_@___@_@@__@@__@__@@_@___@@@@__@@___@_@@\n" +
                "_@@@@__@@___@@__@_@@@__@@_____@___@_@____@@@@\n" +
                "@__@@_@_@@@@_@@__@@@@@@@@__@__@_@_@@@@@@@@_@@\n" +
                "________@_@____@@___@___@____@@_____@___@@___\n" +
                "@@@@@@@___@_@__@___@@_@_@@_@_@_@@_@@@_@_@_@__\n" +
                "@_____@_@__@@_@@@__@@___@__@@@_@@@@@@___@_@@@\n" +
                "@_@@@_@_@@@@@_@@__@@@@@@@@__@@@_@@__@@@@@@_@@\n" +
                "@_@@@_@_@_@__@_@_@_@_@_@_@@@_@__@@@@__@_@@_@@\n" +
                "@_@@@_@__@_@_@_@___@__@__@@@____@___@_@@_@@@@\n" +
                "@_____@_@@_@@@_@@@____@__@_@__@@@__@@@__@@@@_\n" +
                "@@@@@@@___@_@@@@__@_@@_@@__@@@@__@___@@_@__@_\n"

        assertEquals(matrix.values().toMatrix(), finalMatrix)
    }
}