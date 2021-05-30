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
 * These are the encoding modes that can be used to generate a qr code
 *
 * Numeric: Only digits allowed\
 * Alphanumeric: Only digits, capital letters and " " (space), "$", "%", "*", "+", "-", ".", "/", ":" are allowed\
 * Binary: Everything allowed\
 * Kanji: Only double-byte Shift JIS chars allowed (byte ranges: 0x8140 - 0x9FFC or 0xE040 - 0xEBBF)
 *
 * @param modeIndicator Each encoding mode has a four-bit mode indicator that identifies it. The encoded data must start with the appropriate mode indicator that specifies the mode being used for the bits that come after it.
 * @param bitsPerDigits How many bits the encoded digit-pairs will have
 * @param digits How many digits (or characters) are paired for encoding
 */
enum class EncodingMode(val modeIndicator: Byte, val bitsPerDigits: Int, val digits: Int) {
    NUMERIC(0b0001, 10, 3),
    ALPHANUMERIC(0b0010, 11, 2),
    BINARY(0b0100, 8, 1),
    KANJI(0b1000, 13, 1);
}