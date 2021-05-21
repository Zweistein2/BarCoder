package de.zweistein2.barcoder

import de.zweistein2.barcoder.qrcode.QRCodeEncoder

interface BarcodeEncoder {
    companion object BarcodeEncoderFactory {
        infix fun fromFormat(format: BarcodeFormat): BarcodeEncoder = getEncoderFromFormat(format)

        fun BarcodeFormat.getEncoder(): BarcodeEncoder = getEncoderFromFormat(this)

        private fun getEncoderFromFormat(format: BarcodeFormat): BarcodeEncoder = when(format) {
            BarcodeFormat.QR_CODE -> QRCodeEncoder()
        }
    }

    fun encode(content: String, parameters: MutableMap<EncodingParameter, String> = mutableMapOf()): Array<BooleanArray>
}