package com.ampairs.printing.render.escpos

import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintDocument
import com.ampairs.printing.core.model.PrintElement
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.ValueFormatter
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.render.Renderer
import com.ampairs.printing.render.thermal.ColumnText
import com.ampairs.printing.render.thermal.ThermalLineComposer

/**
 * Renders a [PrintDocument] to an ESC/POS byte stream for thermal printers. Pure (no IO) so it is
 * covered by golden-file tests. Column alignment is composed in software ([ThermalLineComposer]) and
 * emitted as left-aligned raw text — identical on every vendor (§8).
 */
class EscPosRenderer : Renderer {

    override val printerClass: PrinterClass = PrinterClass.THERMAL

    override fun render(
        document: PrintDocument,
        profile: PrinterProfile,
        formatter: ValueFormatter,
    ): RenderedOutput {
        val width = columnsPerLine(profile)
        val out = ByteWriter()
        out += EscPos.INIT

        for (element in document.blocks) {
            renderElement(element, width, profile, formatter, out)
        }
        // Feed past the cutter and cut if supported.
        out += EscPos.feed(3)
        if (profile.capabilities.supportsCut) out += EscPos.cut(partial = true)

        return RenderedOutput.Bytes(out.toByteArray())
    }

    private fun renderElement(
        element: PrintElement,
        width: Int,
        profile: PrinterProfile,
        formatter: ValueFormatter,
        out: ByteWriter,
    ) {
        when (element) {
            is PrintElement.TextLine -> {
                val text = formatter.format(element.content)
                emitStyledLines(
                    ThermalLineComposer.line(text, width, element.style.align),
                    element.style.bold, element.style.widthScale, element.style.heightScale, out,
                )
            }

            is PrintElement.KeyValueRow -> {
                val label = formatter.format(element.label)
                val value = formatter.format(element.value)
                val lines = ThermalLineComposer.composeRow(
                    listOf(
                        ColumnText(label, weight = 1, align = Align.LEFT),
                        ColumnText(value, weight = 1, align = Align.RIGHT),
                    ),
                    width,
                )
                emitStyledLines(lines, element.style.bold, element.style.widthScale, element.style.heightScale, out)
            }

            is PrintElement.Table -> {
                val weights = element.columns.map { it.style.weight }
                val aligns = element.columns.map { it.style.align }
                // Header
                val headerCells = element.columns.mapIndexed { i, c ->
                    ColumnText(c.title, weights[i], aligns[i])
                }
                emitStyledLines(ThermalLineComposer.composeRow(headerCells, width), bold = true, 1, 1, out)
                out += EscPos.text("-".repeat(width)); out += EscPos.NEWLINE
                // Rows
                for (row in element.rows) {
                    val cells = row.mapIndexed { i, v ->
                        ColumnText(formatter.format(v), weights.getOrElse(i) { 1 }, aligns.getOrElse(i) { Align.LEFT })
                    }
                    emitStyledLines(ThermalLineComposer.composeRow(cells, width), bold = false, 1, 1, out)
                }
            }

            is PrintElement.Divider -> {
                out += EscPos.text(element.char.toString().repeat(width)); out += EscPos.NEWLINE
            }

            is PrintElement.Spacer -> out += EscPos.feed(element.lines)
            is PrintElement.Feed -> out += EscPos.feed(element.lines)

            is PrintElement.Qr -> {
                // Native QR when supported; otherwise a readable text fallback (raster comes later).
                if (profile.capabilities.supportsNativeQr) {
                    out += qrCommand(element.value, element.sizeModule)
                } else {
                    emitStyledLines(ThermalLineComposer.line("[QR] ${element.value}", width, Align.LEFT), false, 1, 1, out)
                }
            }

            is PrintElement.Barcode -> {
                val cmd = if (profile.capabilities.supportsNativeBarcode) {
                    EscPos.barcode(element.value, element.symbology)
                } else {
                    null
                }
                if (cmd != null) {
                    // Center the symbol, print HRI digits below, then restore left alignment.
                    out += EscPos.align(Align.CENTER)
                    out += EscPos.barcodeHeight(element.heightDots)
                    out += EscPos.barcodeWidth(2)
                    out += EscPos.barcodeHriBelow()
                    out += cmd
                    out += EscPos.NEWLINE
                    out += EscPos.align(Align.LEFT)
                } else {
                    // No native 1D barcode (or non-1D symbology) — readable text so the value is never lost.
                    emitStyledLines(ThermalLineComposer.line("[BARCODE] ${element.value}", width, Align.CENTER), false, 1, 1, out)
                }
            }

            is PrintElement.Image -> {
                // Logo raster is a follow-up; emit nothing rather than corrupt output.
            }

            is PrintElement.Cut -> {
                if (profile.capabilities.supportsCut) out += EscPos.cut(element.partial)
            }

            is PrintElement.CashDrawerKick -> {
                if (profile.capabilities.supportsCashDrawer) out += EscPos.CASH_DRAWER
            }
        }
    }

    private fun emitStyledLines(
        lines: List<String>,
        bold: Boolean,
        widthScale: Int,
        heightScale: Int,
        out: ByteWriter,
    ) {
        val styled = bold || widthScale > 1 || heightScale > 1
        if (styled) {
            if (bold) out += EscPos.bold(true)
            if (widthScale > 1 || heightScale > 1) out += EscPos.size(widthScale, heightScale)
        }
        for (line in lines) {
            out += EscPos.text(line)
            out += EscPos.NEWLINE
        }
        if (styled) {
            if (widthScale > 1 || heightScale > 1) out += EscPos.size(1, 1)
            if (bold) out += EscPos.bold(false)
        }
    }

    /** GS ( k — store + print a QR code. Model 2, the given module size, error-correction M. */
    private fun qrCommand(data: String, module: Int): ByteArray {
        val bytes = data.encodeToByteArray()
        val store = ByteArray(bytes.size + 3)
        val len = bytes.size + 3
        val pL = (len % 256).toByte()
        val pH = (len / 256).toByte()
        val w = ByteWriter()
        // Model 2
        w += byteArrayOf(EscPos.GS, '('.code.toByte(), 'k'.code.toByte(), 4, 0, 49, 65, 50, 0)
        // Module size
        w += byteArrayOf(EscPos.GS, '('.code.toByte(), 'k'.code.toByte(), 3, 0, 49, 67, module.coerceIn(1, 16).toByte())
        // Error correction = M (49)
        w += byteArrayOf(EscPos.GS, '('.code.toByte(), 'k'.code.toByte(), 3, 0, 49, 69, 49)
        // Store data
        w += byteArrayOf(EscPos.GS, '('.code.toByte(), 'k'.code.toByte(), pL, pH, 49, 80, 48)
        w += bytes
        // Print
        w += byteArrayOf(EscPos.GS, '('.code.toByte(), 'k'.code.toByte(), 3, 0, 49, 81, 48)
        // silence unused warnings
        store[0] = 0
        return w.toByteArray()
    }

    private fun columnsPerLine(profile: PrinterProfile): Int {
        val cap = profile.capabilities.charsPerLine
        if (cap > 0) return cap
        return when (val p = profile.paper) {
            is PaperSpec.Thermal -> p.widthChars
            else -> 48
        }
    }
}

/** Minimal growable byte buffer (avoids platform deps in commonMain). */
internal class ByteWriter {
    private var buf = ByteArray(256)
    private var len = 0

    operator fun plusAssign(bytes: ByteArray) {
        ensure(len + bytes.size)
        bytes.copyInto(buf, len)
        len += bytes.size
    }

    private fun ensure(capacity: Int) {
        if (capacity <= buf.size) return
        var newSize = buf.size * 2
        while (newSize < capacity) newSize *= 2
        buf = buf.copyOf(newSize)
    }

    fun toByteArray(): ByteArray = buf.copyOf(len)
}
