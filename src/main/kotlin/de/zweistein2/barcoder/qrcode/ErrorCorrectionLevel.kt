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
 * These are the error correction levels that can be used to generate a qr code
 *
 * Mode L is capable of recovering the data if up to 7% of the qr code is lost\
 * Mode M is capable of recovering the data if up to 15% of the qr code is lost\
 * Mode Q is capable of recovering the data if up to 25% of the qr code is lost\
 * Mode H is capable of recovering the data if up to 30% of the qr code is lost
 *
 * @param bitSequence The binary string representation of the error correction level (needed to generate the appropriate format bits)
 */
enum class ErrorCorrectionLevel(val bitSequence: String) {
    L("01"),
    M("00"),
    Q("11"),
    H("10")
}