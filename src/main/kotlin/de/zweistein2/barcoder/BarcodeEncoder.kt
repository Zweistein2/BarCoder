package de.zweistein2.barcoder

import de.zweistein2.barcoder.qrcode.QRCodeEncoder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

interface BarcodeEncoder {
    companion object BarcodeEncoderFactory {
        infix fun fromFormat(format: BarcodeFormat): BarcodeEncoder = getEncoderFromFormat(format)

        fun BarcodeFormat.getEncoder(): BarcodeEncoder = getEncoderFromFormat(this)

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

        fun BufferedImage.toPng(filename: String) {
            ImageIO.write(this, "png", File(filename))
        }

        private fun getEncoderFromFormat(format: BarcodeFormat): BarcodeEncoder = when(format) {
            BarcodeFormat.QR_CODE -> QRCodeEncoder()
        }
    }

    fun encode(content: String, parameters: MutableMap<EncodingParameter, String> = mutableMapOf()): Array<BooleanArray>

}