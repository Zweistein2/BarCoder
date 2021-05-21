package de.zweistein2.barcoder.qrcode

enum class EncodingMode(val modeIndicator: Byte, val bitsPerDigits: Int, val digits: Int) {
    NUMERIC(0b0001, 10, 3),
    ALPHANUMERIC(0b0010, 11, 2),
    BINARY(0b0100, 8, 1),
    KANJI(0b1000, 13, 1);
}