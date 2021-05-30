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

/**
 * The mask patterns used for qr code generation
 *
 * @param bitSequence The binary string representation of the mask (needed to generate the appropriate format bits)
 * @param formula The formula used to mask the data (and error correction) bits
 */
enum class MaskPattern(val bitSequence: String, val formula: (Int, Int) -> Boolean) {
    PATTERN_1("000", { row, col -> (row + col) % 2 == 0 } ),
    PATTERN_2("001", { row, _ -> row % 2 == 0 }),
    PATTERN_3("010", { _, col -> col % 3 == 0 }),
    PATTERN_4("011", { row, col -> (row + col) % 3 == 0 }),
    PATTERN_5("100", { row, col -> (row / 2 + col / 3) % 2 == 0 }),
    PATTERN_6("101", { row, col -> (row * col) % 2 + (row * col) % 3 == 0 }),
    PATTERN_7("110", { row, col -> ((row * col) % 3 + row * col) % 2 == 0 }),
    PATTERN_8("111", { row, col -> ((row * col) % 3 + row + col) % 2 == 0 });
}