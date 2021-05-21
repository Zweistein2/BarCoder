package de.zweistein2.barcoder.qrcode

import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.reserved
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.roundDownToEvenInt
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.toBinaryString
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.values
import kotlin.math.roundToInt

object MatrixUtil {
    fun initiateMatrix(version: Int): Map<String, Array<BooleanArray>> {
        val size = version * 4 + 17
        val matrix = mapOf(
            Pair("Values", Array(size) { BooleanArray(size) }),
            Pair("Reserved", Array(size) { BooleanArray(size) })
        )

        val topLeftCoordinates = Pair(0, 0)
        val topRightCoordinates = Pair(size - 7, 0)
        val bottomLeftCoordinates = Pair(0, size - 7)

        val finderPatternCoordinates = listOf(topLeftCoordinates, topRightCoordinates, bottomLeftCoordinates)
        val alignmentPatternCoordinates = getAlignmentCoordinates(version)

        generateTimingPattern(matrix.values())
        reserveTimingPattern(matrix.reserved())

        for(finderPatternCoordinatePair in finderPatternCoordinates) {
            generateFinderPattern(matrix.values(), finderPatternCoordinatePair.first, finderPatternCoordinatePair.second)
            reserveFinderPattern(matrix.reserved(), finderPatternCoordinatePair.first, finderPatternCoordinatePair.second)

            reserveVersionPatternIfNecessary(version, matrix.reserved(), finderPatternCoordinatePair.first, finderPatternCoordinatePair.second)
        }

        for(alignmentPatternCoordinatePair in alignmentPatternCoordinates) {
            generateAlignmentPattern(matrix.values(), alignmentPatternCoordinatePair.first, alignmentPatternCoordinatePair.second)
            reserveAlignmentPattern(matrix.reserved(), alignmentPatternCoordinatePair.first, alignmentPatternCoordinatePair.second)
        }

        return matrix
    }

    fun getAlignmentCoordinates(version: Int): List<Pair<Int, Int>> {
        val alignmentCoordinates = mutableListOf<Pair<Int, Int>>()
        val size = version * 4 + 17

        if(version > 1) {
            val alignmentRowCount = (version / 7.0).toInt() + 2

            val coords = mutableListOf<Int>()

            val firstCoords = 6
            coords.add(firstCoords)

            val lastCoords = size - 1 - firstCoords
            val secondLastCoords = ((firstCoords + lastCoords * (alignmentRowCount - 2) + (alignmentRowCount - 1) / 2.0).roundToInt() / (alignmentRowCount - 1.0)).roundDownToEvenInt()
            val posStep = lastCoords - secondLastCoords
            val secondCoords = lastCoords - (alignmentRowCount -2) * posStep

            for(i in secondCoords..lastCoords+1 step posStep) {
                coords.add(i)
            }

            for(i in 0 until alignmentRowCount) {
                for(j in 0 until alignmentRowCount) {
                    alignmentCoordinates.add(Pair(coords[i], coords[j]))
                }
            }
        }

        alignmentCoordinates.removeIf { it.first in 0..8 && it.second in 0..8 }  // remove overlapping alignmentCoords in top-left Finder Blocks
        alignmentCoordinates.removeIf { it.first in size-8..size && it.second in 0..8 }  // remove overlapping alignmentCoords in top-right Finder Blocks
        alignmentCoordinates.removeIf { it.first in 0..8 && it.second in size-8..size }  // remove overlapping alignmentCoords in bottom-left Finder Blocks

        return alignmentCoordinates
    }

    fun placeFormatInMatrix(matrix: Map<String, Array<BooleanArray>>, formatBits: String) {
        val size = matrix.values().size

        for(i in 0 until 15) {
            val bit = formatBits[i] == '1'

            when {
                i < 6 -> {
                    matrix.values()[8][i] = bit
                    matrix.values()[size-(i+1)][8] = bit
                }
                i == 6 -> {
                    matrix.values()[8][7] = bit
                    matrix.values()[size-7][8] = bit
                }
                i == 7 -> {
                    matrix.values()[8][8] = formatBits[7] == '1'
                    matrix.values()[8][size-8] = formatBits[7] == '1'
                }
                i == 8 -> {
                    matrix.values()[7][8] = bit
                    matrix.values()[8][size-7] = bit
                }
                i > 8 -> {
                    matrix.values()[14-i][8] = bit
                    matrix.values()[8][size-(14-i+1)] = bit
                }
            }
        }
    }

    fun placeVersionInMatrix(matrix: Map<String, Array<BooleanArray>>, versionBits: String) {
        val size = matrix.values().size

        var counter = 17

        for(y in 0 until 6) {
            for(x in 2 downTo 0) {
                matrix.values()[y][size-(9+x)] = versionBits[counter] == '1'
                counter--
            }
        }

        counter = 17

        for(x in 0 until 6) {
            for(y in 2 downTo 0) {
                // Hier werden alle mit "Bit" überschrieben
                matrix.values()[size-(9+y)][x] = versionBits[counter] == '1'
                counter--
            }
        }
    }

    fun placePayloadInMatrix(matrix: Map<String, Array<BooleanArray>>, finalPayload: MutableList<Int>) {
        val payloadBits = finalPayload.stream().map { it.toBinaryString(8) }.reduce { s1, s2 -> s1 + s2 }.orElse("")
        val size = matrix.values().size

        var direction = -1
        var bitIndex = 0
        var x = size - 1
        var y = size - 1

        while(x > 0) {
            // Skip the vertical timing pattern.
            if (x == 6) {
                x -= 1
            }
            while(y in 0 until size) {
                for(i in 0 until 2) {
                    var bit: Boolean
                    val tempX = x - i

                    if(matrix.reserved()[y][tempX]) {
                        continue
                    }

                    if(bitIndex < payloadBits.length) {
                        bit = payloadBits[bitIndex] == '1'
                        ++bitIndex
                    } else {
                        //pad with 0
                        bit = false
                    }

                    matrix.values()[y][tempX] = bit
                }

                y += direction
            }

            direction = -direction
            y += direction
            x -= 2
        }
    }

    private fun generateAlignmentPattern(matrix: Array<BooleanArray>, x: Int, y: Int) {
        for((rowIndex, row) in matrix.withIndex()) {
            for((colIndex, _) in row.withIndex()) {
                matrix[rowIndex][colIndex] = when {
                    rowIndex == y && colIndex == x -> true
                    rowIndex in y-1..y+1 && colIndex in x-1..x+1 -> false
                    rowIndex in y-2..y+2 && colIndex in x-2..x+2 -> true
                    else -> matrix[rowIndex][colIndex]
                }
            }
        }
    }

    private fun reserveAlignmentPattern(reserved: Array<BooleanArray>, x: Int, y: Int) {
        for((rowIndex, row) in reserved.withIndex()) {
            for((colIndex, _) in row.withIndex()) {
                reserved[rowIndex][colIndex] = when {
                    rowIndex in y - 2..y + 2 && colIndex in x - 2..x + 2 -> true
                    else -> reserved[rowIndex][colIndex]
                }
            }
        }
    }

    private fun generateFinderPattern(matrix: Array<BooleanArray>, x: Int, y: Int) {
        for((rowIndex, row) in matrix.withIndex()) {
            for((colIndex, _) in row.withIndex()) {
                matrix[rowIndex][colIndex] = when {
                    rowIndex in y+2..y+4 && colIndex in x+2..x+4 -> true
                    rowIndex in y+1..y+5 && colIndex in x+1..x+5 -> false
                    rowIndex in y+0..y+6 && colIndex in x+0..x+6 -> true
                    rowIndex == y-1 && colIndex == 8 && y > 0 && x == 0 -> true
                    else -> matrix[rowIndex][colIndex]
                }
            }
        }
    }

    private fun reserveFinderPattern(reserved: Array<BooleanArray>, x: Int, y: Int) {
        for((rowIndex, row) in reserved.withIndex()) {
            for((colIndex, _) in row.withIndex()) {
                reserved[rowIndex][colIndex] = when {
                    rowIndex in y..y+8 && colIndex in x-1..x+7 && y == 0 && x > 0 -> true
                    rowIndex in y-1..y+7 && colIndex in x..x+8 && y > 0 && x == 0 -> true
                    rowIndex in y..y+8 && colIndex in x..x+8 -> true
                    else -> reserved[rowIndex][colIndex]
                }
            }
        }
    }

    private fun generateTimingPattern(matrix: Array<BooleanArray>) {
        for((rowIndex, row) in matrix.withIndex()) {
            for((colIndex, _) in row.withIndex()) {
                matrix[rowIndex][colIndex] = when {
                    colIndex == 6 && rowIndex % 2 == 0 -> true
                    rowIndex == 6 && colIndex % 2 == 0 -> true
                    else -> matrix[rowIndex][colIndex]
                }
            }
        }
    }

    private fun reserveTimingPattern(reserved: Array<BooleanArray>) {
        for((rowIndex, row) in reserved.withIndex()) {
            for((colIndex, _) in row.withIndex()) {
                reserved[rowIndex][colIndex] = when {
                    colIndex == 6 -> true
                    rowIndex == 6 -> true
                    else -> reserved[rowIndex][colIndex]
                }
            }
        }
    }

    private fun reserveVersionPatternIfNecessary(version: Int, reserved: Array<BooleanArray>, x: Int, y: Int) {
        if (version < 7) {
            return
        }

        for((rowIndex, row) in reserved.withIndex()) {
            for((colIndex, _) in row.withIndex()) {
                reserved[rowIndex][colIndex] = when {
                    rowIndex in y..y+5 && colIndex in x-4 until x && y == 0 && x > 0 -> true // top-right
                    rowIndex in y-4 until y && colIndex in x..x+5 && y > 0 && x == 0 -> true // bottom-left
                    else -> reserved[rowIndex][colIndex]
                }
            }
        }
    }
}