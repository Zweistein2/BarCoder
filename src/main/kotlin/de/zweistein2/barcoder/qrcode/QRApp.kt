package de.zweistein2.barcoder.qrcode

import tornadofx.App
import tornadofx.launch

fun main(args: Array<String>) {
    launch<QRApp>(args)
}

class QRApp: App(QRView::class)