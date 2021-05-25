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

enum class ErrorCorrectionLevel(val bitSequence: String) {
    L("01"),      // Low          7%  of data bytes can be restored.
    M("00"),      // Medium       15% of data bytes can be restored.
    Q("11"),      // Quartile     25% of data bytes can be restored.
    H("10")       // High         30% of data bytes can be restored.
}