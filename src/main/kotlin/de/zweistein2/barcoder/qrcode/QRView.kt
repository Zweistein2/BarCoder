package de.zweistein2.barcoder.qrcode

import de.zweistein2.barcoder.BarcodeEncoder.BarcodeEncoderFactory.getEncoder
import de.zweistein2.barcoder.BarcodeEncoder.BarcodeEncoderFactory.toBufferedImage
import de.zweistein2.barcoder.BarcodeEncoder.BarcodeEncoderFactory.toPng
import de.zweistein2.barcoder.BarcodeFormat
import de.zweistein2.barcoder.EncodingParameter
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import tornadofx.*


class QRView: View() {
    override val root = stackpane {
        group {
            val encoder = BarcodeFormat.QR_CODE.getEncoder() as QRCodeEncoder
            val matrix = encoder.encode("https://github.com/Zweistein2/BarCoder", mutableMapOf(Pair(EncodingParameter.ERROR_CORRECTION_LEVEL, ErrorCorrectionLevel.M.name)))

            val img = matrix.toBufferedImage(10)

            val wr = WritableImage(img.width, img.height)
            val pw: PixelWriter = wr.pixelWriter
            for (x in 0 until img.width) {
                for (y in 0 until img.height) {
                    pw.setArgb(x, y, img.getRGB(x, y))
                }
            }

            imageview {
                image = wr
            }
        }
    }
}