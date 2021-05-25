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

enum class EncodingMode(val modeIndicator: Byte, val bitsPerDigits: Int, val digits: Int) {
    NUMERIC(0b0001, 10, 3),
    ALPHANUMERIC(0b0010, 11, 2),
    BINARY(0b0100, 8, 1),
    KANJI(0b1000, 13, 1);
}