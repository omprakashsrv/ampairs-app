package com.ampairs.printing.render.label

import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PrintDocument
import com.ampairs.printing.core.model.PrintElement
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.ValueFormatter
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.render.Renderer

/**
 * Renders a [PrintDocument] to a TSPL label program (common label-printer dialect). One label per
 * document; barcodes/QR map to native TSPL commands. Pure (no IO), golden-testable.
 */
class LabelRenderer : Renderer {

    override val printerClass: PrinterClass = PrinterClass.LABEL

    override fun render(
        document: PrintDocument,
        profile: PrinterProfile,
        formatter: ValueFormatter,
    ): RenderedOutput {
        val label = profile.paper as? PaperSpec.Label ?: PaperSpec.Label(50.0, 30.0)
        val sb = StringBuilder()
        sb.append("SIZE ${fmt(label.widthMm)} mm,${fmt(label.heightMm)} mm\r\n")
        sb.append("GAP 2 mm,0 mm\r\n")
        sb.append("DIRECTION 1\r\n")
        sb.append("CLS\r\n")

        var y = 10
        val xDots = 10
        for (element in document.blocks) {
            when (element) {
                is PrintElement.TextLine -> {
                    sb.append("TEXT $xDots,$y,\"3\",0,1,1,\"${tsplEsc(formatter.format(element.content))}\"\r\n")
                    y += 30
                }
                is PrintElement.KeyValueRow -> {
                    val line = "${formatter.format(element.label)} ${formatter.format(element.value)}"
                    sb.append("TEXT $xDots,$y,\"3\",0,1,1,\"${tsplEsc(line)}\"\r\n")
                    y += 30
                }
                is PrintElement.Barcode -> {
                    sb.append("BARCODE $xDots,$y,\"128\",60,1,0,2,2,\"${tsplEsc(element.value)}\"\r\n")
                    y += 80
                }
                is PrintElement.Qr -> {
                    sb.append("QRCODE $xDots,$y,M,${element.sizeModule.coerceIn(1, 10)},A,0,\"${tsplEsc(element.value)}\"\r\n")
                    y += 80
                }
                is PrintElement.Divider -> y += 10
                is PrintElement.Spacer -> y += element.lines * 20
                is PrintElement.Feed -> y += element.lines * 20
                else -> Unit // Table/Image/Cut/CashDrawer not used on labels
            }
        }
        sb.append("PRINT ${document.meta.copies.coerceAtLeast(1)}\r\n")
        return RenderedOutput.Bytes(sb.toString().encodeToByteArray())
    }

    private fun fmt(v: Double): String {
        val whole = v.toLong()
        return if (v == whole.toDouble()) whole.toString() else v.toString()
    }

    private fun tsplEsc(s: String): String = s.replace("\"", "\\\"")
}
