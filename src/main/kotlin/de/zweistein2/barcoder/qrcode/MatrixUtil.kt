/*
 * Copyright 2021 Fabian Karolat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.zweistein2.barcoder.qrcode

import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.reserved
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.roundDownToEvenInt
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.toBinaryString
import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.values
import kotlin.math.roundToInt

/**
 * A utility class for all matrix-related functions
 */
object MatrixUtil {
    /**
     * This method initializes a matrix with the basic function patterns and the corresponding reserved areas applied
     *
     * @param version The version of the qr code (needed for size and calculation of alignment patterns)
     * @return A map containing two matrices. One for the data and one for the reserved areas
     */
    fun initializeMatrix(version: Int): Map<String, Array<BooleanArray>> {
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

    /**
     * This method calculates the coordinates of the alignment patterns
     *
     * @param version The version for which the alignment patterns shall be calculated
     * @return A list of coordinates
     */
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

    /**
     * This method places the format bits into the matrix
     *
     * @param matrix The data values of the matrix
     * @param formatBits The binary string representation of the format bits
     */
    fun placeFormatInMatrix(matrix: Array<BooleanArray>, formatBits: String) {
        val size = matrix.size

        for(i in 0 until 15) {
            val bit = formatBits[i] == '1'

            when {
                i < 6 -> {
                    matrix[8][i] = bit
                    matrix[size-(i+1)][8] = bit
                }
                i == 6 -> {
                    matrix[8][7] = bit
                    matrix[size-7][8] = bit
                }
                i == 7 -> {
                    matrix[8][8] = formatBits[7] == '1'
                    matrix[8][size-8] = formatBits[7] == '1'
                }
                i == 8 -> {
                    matrix[7][8] = bit
                    matrix[8][size-7] = bit
                }
                i > 8 -> {
                    matrix[14-i][8] = bit
                    matrix[8][size-(14-i+1)] = bit
                }
            }
        }
    }

    /**
     * This method places the version bits into the matrix
     *
     * @param matrix The data values of the matrix
     * @param versionBits The binary string representation of the version bits
     */
    fun placeVersionInMatrix(matrix: Array<BooleanArray>, versionBits: String) {
        val size = matrix.size

        var counter = 17

        for(y in 0 until 6) {
            for(x in 2 downTo 0) {
                matrix[y][size-(9+x)] = versionBits[counter] == '1'
                counter--
            }
        }

        counter = 17

        for(x in 0 until 6) {
            for(y in 2 downTo 0) {
                // Hier werden alle mit "Bit" ??berschrieben
                matrix[size-(9+y)][x] = versionBits[counter] == '1'
                counter--
            }
        }
    }

    /**
     * This method places the payload bits into the matrix
     *
     * @param matrix The map containing the data matrix and the reserved area matrix
     * @param finalPayload The payload bits as a list of 8-bit numbers
     */
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

    /**
     * This method generates an alignment pattern at the provided coordinates
     *
     * @param matrix The data values of the matrix
     * @param x The x coordinate where the alignment pattern shall be generated
     * @param y The y coordinate where the alignment pattern shall be generated
     */
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

    /**
     * This method reserves the area of an alignment pattern at the provided coordinates
     *
     * @param reserved The reserved area values of the matrix
     * @param x The x coordinate where the alignment pattern shall be generated
     * @param y The y coordinate where the alignment pattern shall be generated
     */
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

    /**
     * This method generates a finder pattern at the provided coordinates
     *
     * @param matrix The data values of the matrix
     * @param x The x coordinate where the finder pattern shall be generated
     * @param y The y coordinate where the finder pattern shall be generated
     */
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

    /**
     * This method reserves the area of a finder pattern at the provided coordinates
     *
     * @param reserved The reserved area values of the matrix
     * @param x The x coordinate where the finder pattern shall be generated
     * @param y The y coordinate where the finder pattern shall be generated
     */
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

    /**
     * This method generates the timing pattern
     *
     * @param matrix The data values of the matrix
     */
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

    /**
     * This method reserves the timing pattern
     *
     * @param reserved The reserved area values of the matrix
     */
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


    /**
     * This method reserves the version pattern if necessary (Only for version 7 and above)
     *
     * @param version The version (used for checking if it is necessary to reserve a version pattern)
     * @param reserved The reserved area values of the matrix
     * @param x The x coordinate where the version pattern shall be generated
     * @param y The y coordinate where the version pattern shall be generated
     */
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