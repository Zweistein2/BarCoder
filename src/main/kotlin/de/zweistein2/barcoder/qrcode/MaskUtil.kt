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

import de.zweistein2.barcoder.qrcode.QRCodeEncoder.Companion.toMatrix
import kotlin.math.min
import kotlin.math.roundToInt

object MaskUtil {
    fun determineMaskPenalty(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, maskPattern: MaskPattern): Int {
        val tempMatrix = Array(matrix.size) { BooleanArray(matrix.size) }
        for ((rowIndex, row) in matrix.withIndex()) {
            for (colIndex in row.indices) {
                tempMatrix[rowIndex][colIndex] = matrix[rowIndex][colIndex]
            }
        }

        for ((rowIndex, row) in matrix.withIndex()) {
            for (colIndex in row.indices) {
                if (!reserved[rowIndex][colIndex]) {
                    tempMatrix[rowIndex][colIndex] = matrix[rowIndex][colIndex] xor maskPattern.formula.invoke(rowIndex, colIndex)
                } else {
                    tempMatrix[rowIndex][colIndex] = matrix[rowIndex][colIndex]
                }
            }
        }

        var penalty = 0

        val tempMatrixAsString = tempMatrix.toMatrix()
        val invertedTempMatrixAsString = flipMatrix(tempMatrix).toMatrix()

        penalty += getFirstPenalty(tempMatrixAsString)
        penalty += getFirstPenalty(invertedTempMatrixAsString)
        penalty += getSecondPenalty(tempMatrix)
        penalty += getThirdPenalty(tempMatrixAsString)
        penalty += getThirdPenalty(invertedTempMatrixAsString)
        penalty += getFourthPenalty(tempMatrix)

        return penalty
    }

    private fun flipMatrix(matrix: Array<BooleanArray>): Array<BooleanArray> {
        val tempMatrix = Array(matrix.size) { BooleanArray(matrix.size) }

        for ((rowIndex, row) in matrix.withIndex()) {
            for (colIndex in row.indices) {
                tempMatrix[colIndex][rowIndex] = matrix[rowIndex][colIndex]
            }
        }

        return tempMatrix
    }

    private fun getFirstPenalty(matrixAsString: String): Int {
        var penalty = 0

        for (line in matrixAsString.split('\n')) {
            val matches = Regex("@{5,}|_{5,}").findAll(line)

            for (match in matches) {
                penalty += match.value.length - 2
            }
        }

        return penalty
    }

    private fun getSecondPenalty(matrix: Array<BooleanArray>): Int {
        var penalty = 0

        for (row in matrix.indices) {
            for (col in matrix[row].indices) {
                if(row >= 1 && col >= 1
                    && matrix[row][col] == matrix[row - 1][col]
                    && matrix[row][col] == matrix[row - 1][col - 1]
                    && matrix[row][col] == matrix[row][col - 1]) {
                    penalty += 3
                }
            }
        }

        return penalty
    }

    private fun getThirdPenalty(matrixAsString: String): Int {
        var penalty = 0

        for (line in matrixAsString.split('\n')) {
            val matches = Regex("@_@@@_@____|____@_@@@_@").findAll(line)

            for (match in matches) {
                penalty += 40
            }
        }

        return penalty
    }

    private fun getFourthPenalty(matrix: Array<BooleanArray>): Int {
        var darkCells = 0

        for (row in matrix.indices) {
            for (col in matrix[row].indices) {
                if(matrix[row][col]) {
                    darkCells++
                }
            }
        }

        val totalCells = matrix.size * matrix.size
        val percentage = darkCells.toDouble() / totalCells.toDouble() * 100
        val nextMultiple = percentage.roundToInt() + 5 - percentage.roundToInt() % 5
        val previousMultiple = percentage.roundToInt() - percentage.roundToInt() % 5

        val nextMultiplePenalty = when {
            nextMultiple < 50 -> { ((nextMultiple - 50) * -1 / 5) * 10 }
            nextMultiple > 50 -> { ((nextMultiple - 50) / 5) * 10 }
            else -> { 0 }
        }
        val previousMultiplePenalty = when {
            previousMultiple < 50 -> { ((previousMultiple - 50) * -1 / 5) * 10 }
            previousMultiple > 50 -> { ((previousMultiple - 50) / 5) * 10 }
            else -> { 0 }
        }

        return min(previousMultiplePenalty, nextMultiplePenalty)
    }
}