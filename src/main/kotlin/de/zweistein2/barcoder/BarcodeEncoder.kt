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

package de.zweistein2.barcoder

import de.zweistein2.barcoder.qrcode.QRCodeEncoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * This interface represents a basic encoder for generating barcodes
 */
interface BarcodeEncoder {
    companion object BarcodeEncoderFactory {
        /**
         * This method returns the corresponding [BarcodeEncoder] for a given [BarcodeFormat]
         *
         * @param format The [BarcodeFormat] for which an encoder shall be found
         * @return The corresponding [BarcodeEncoder]
         */
        infix fun fromFormat(format: BarcodeFormat): BarcodeEncoder = getEncoderFromFormat(format)

        /**
         * This method returns the corresponding [BarcodeEncoder] for the [BarcodeFormat]
         *
         * @return The corresponding [BarcodeEncoder]
         */
        fun BarcodeFormat.getEncoder(): BarcodeEncoder = getEncoderFromFormat(this)

        /**
         * This method maps a matrix to a [BufferedImage]
         *
         * @param pixelSize The size in pixels on module of the barcode should have
         * @return The corresponding [BufferedImage]
         */
        fun Array<BooleanArray>.toBufferedImage(pixelSize: Int): BufferedImage {
            val img = BufferedImage((this.size + 8) * pixelSize, (this.size + 8) * pixelSize, 1)

            for (rowIndex in -4..this.size+4) {
                val row = if (rowIndex in this.indices) {
                    this[rowIndex]
                } else {
                    BooleanArray(this.size)
                }

                for (colIndex in -4..row.size+4) {
                    if (colIndex !in row.indices || rowIndex !in this.indices || !this[rowIndex][colIndex]) {
                        img.graphics.fillRect((4+colIndex)*pixelSize, (4+rowIndex)*pixelSize, pixelSize, pixelSize)
                    }
                }
            }

            return img
        }

        /**
         * This method stores a [BufferedImage] as a png in the filesystem
         *
         * @param filename The path and filename under which the png should be stored
         */
        fun BufferedImage.toPng(filename: String) {
            ImageIO.write(this, "png", File(filename))
        }

        /**
         * This method maps a [BarcodeFormat] to an encoder
         *
         * @param format The [BarcodeFormat] for which an encoder shall be found
         * @return The corresponding [BarcodeEncoder]
         */
        private fun getEncoderFromFormat(format: BarcodeFormat): BarcodeEncoder = when(format) {
            BarcodeFormat.QR_CODE -> QRCodeEncoder()
        }
    }

    /**
     * This method encodes a given content into a barcode-matrix\
     * It's possible to use additional parameters to configure the encoding
     *
     * @param content The content which shall be encoded
     * @param parameters The Parameters to be used for encoding
     * @return A barcode-matrix
     */
    fun encode(content: String, parameters: MutableMap<EncodingParameter, String> = mutableMapOf()): Array<BooleanArray>
}