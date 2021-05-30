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
 * These are the charsets that can be used to generate a qr code
 *
 * Kanji can be used with UTF_8 or SHIFT_JIS where SHIFT_JIS yields the higher capacity
 */
enum class Charset {
    UTF_8,
    ISO_8859_1,
    SHIFT_JIS
}