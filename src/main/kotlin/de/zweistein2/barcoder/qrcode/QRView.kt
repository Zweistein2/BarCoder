package de.zweistein2.barcoder.qrcode

import de.zweistein2.barcoder.BarcodeEncoder.BarcodeEncoderFactory.getEncoder
import de.zweistein2.barcoder.BarcodeFormat
import de.zweistein2.barcoder.EncodingParameter
import tornadofx.*

class QRView: View() {
    override val root = stackpane {
        group {
            val encoder = BarcodeFormat.QR_CODE.getEncoder() as QRCodeEncoder
            val matrix = encoder.encode("https://github.com/Zweistein2/BarCoder", mutableMapOf(Pair(EncodingParameter.ERROR_CORRECTION_LEVEL, ErrorCorrectionLevel.M.name)))

            val size = 10.0

            for (rowIndex in -4..matrix.size+4) {
                val row = if(rowIndex in matrix.indices) { matrix[rowIndex] } else { BooleanArray(matrix.size) }

                for (colIndex in -4..row.size+4) {
                    rectangle {
                        fill = if (colIndex in row.indices && rowIndex in matrix.indices && matrix[rowIndex][colIndex]) { javafx.scene.paint.Color.BLACK } else { javafx.scene.paint.Color.WHITE }
                        width = size
                        height = size
                        x = colIndex * size
                        y = rowIndex * size
                    }
                }
            }
        }
    }
}