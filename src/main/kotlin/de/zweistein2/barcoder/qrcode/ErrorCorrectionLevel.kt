package de.zweistein2.barcoder.qrcode

enum class ErrorCorrectionLevel(val bitSequence: String) {
    L("01"),      // Low          7%  of data bytes can be restored.
    M("00"),      // Medium       15% of data bytes can be restored.
    Q("11"),      // Quartile     25% of data bytes can be restored.
    H("10")       // High         30% of data bytes can be restored.
}