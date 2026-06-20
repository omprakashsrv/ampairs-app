package com.ampairs.printing.render.html

import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.Orientation
import com.ampairs.printing.core.model.PageSize
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintDocument
import com.ampairs.printing.core.model.PrintElement
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.ValueFormatter
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.render.Renderer

/**
 * Renders a [PrintDocument] to a self-contained HTML document for page printers (inkjet/laser),
 * handed to the OS print service. CSS `@page` is sized from the [PaperSpec]; `<thead>` repeats per
 * printed page. Pure (no IO) — generalizes the legacy `buildInvoiceHtml`.
 */
class HtmlRenderer : Renderer {

    override val printerClass: PrinterClass = PrinterClass.PAGE

    override fun render(
        document: PrintDocument,
        profile: PrinterProfile,
        formatter: ValueFormatter,
    ): RenderedOutput {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\">")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        sb.append("<style>").append(css(profile.paper)).append("</style></head><body><div class=\"doc\">")
        for (element in document.blocks) renderElement(element, formatter, sb)
        sb.append("</div></body></html>")
        return RenderedOutput.Markup(sb.toString())
    }

    private fun renderElement(element: PrintElement, formatter: ValueFormatter, sb: StringBuilder) {
        when (element) {
            is PrintElement.TextLine -> {
                val cls = styleClass(element.style.bold, element.style.align)
                sb.append("<p class=\"$cls\">").append(esc(formatter.format(element.content))).append("</p>")
            }

            is PrintElement.KeyValueRow -> {
                sb.append("<div class=\"kv\"><span>").append(esc(formatter.format(element.label)))
                    .append("</span><span>").append(esc(formatter.format(element.value))).append("</span></div>")
            }

            is PrintElement.Table -> {
                // Size columns by their weight so narrow numeric columns (Qty/Rate/Amount) stay
                // visible instead of being squeezed out by a long item description (table-layout:fixed).
                val total = element.columns.sumOf { it.style.weight }.coerceAtLeast(1)
                sb.append("<table class=\"items\"><colgroup>")
                for (c in element.columns) {
                    sb.append("<col style=\"width:").append(c.style.weight * 100 / total).append("%\">")
                }
                sb.append("</colgroup><thead><tr>")
                for (c in element.columns) {
                    // width attr too — desktop JEditorPane (HTML 3.2) ignores <colgroup>/table-layout.
                    sb.append("<th width=\"").append(c.style.weight * 100 / total)
                        .append("%\" class=\"${alignClass(c.style.align)}\">").append(esc(c.title)).append("</th>")
                }
                sb.append("</tr></thead><tbody>")
                for (row in element.rows) {
                    sb.append("<tr>")
                    row.forEachIndexed { i, v ->
                        val a = element.columns.getOrNull(i)?.style?.align ?: Align.LEFT
                        sb.append("<td class=\"${alignClass(a)}\">").append(esc(formatter.format(v))).append("</td>")
                    }
                    sb.append("</tr>")
                }
                sb.append("</tbody></table>")
            }

            is PrintElement.Grid -> {
                val cols = element.columns.coerceAtLeast(1)
                sb.append("<table class=\"grid\"><tbody>")
                element.cells.chunked(cols).forEach { rowCells ->
                    sb.append("<tr>")
                    for (cell in rowCells) {
                        sb.append("<td>")
                        if (cell.label.isNotBlank()) {
                            sb.append("<span class=\"gl\">").append(esc(cell.label)).append("</span> ")
                        }
                        sb.append(esc(formatter.format(cell.value))).append("</td>")
                    }
                    repeat(cols - rowCells.size) { sb.append("<td></td>") }
                    sb.append("</tr>")
                }
                sb.append("</tbody></table>")
            }

            is PrintElement.Divider -> sb.append("<hr>")
            is PrintElement.Spacer -> repeat(element.lines) { sb.append("<br>") }
            is PrintElement.Feed -> repeat(element.lines) { sb.append("<br>") }
            is PrintElement.Image -> {
                // Only emit a real URL; an unresolved cache-key ref (e.g. "business_logo") would make
                // the OS HTML printer (JEditorPane) throw trying to resolve it, failing the whole job.
                val ref = element.ref
                if (ref.startsWith("http://") || ref.startsWith("https://") ||
                    ref.startsWith("data:") || ref.startsWith("file:")
                ) {
                    sb.append("<img class=\"logo\" src=\"").append(esc(ref)).append("\">")
                }
            }
            is PrintElement.Barcode -> sb.append("<div class=\"code\">").append(esc(element.value)).append("</div>")
            is PrintElement.Qr -> sb.append("<div class=\"qr\">").append(esc(element.value)).append("</div>")
            is PrintElement.Cut -> Unit          // page printers do not cut
            is PrintElement.CashDrawerKick -> Unit
        }
    }

    private fun styleClass(bold: Boolean, align: Align): String =
        (if (bold) "b " else "") + alignClass(align)

    private fun alignClass(align: Align): String = when (align) {
        Align.LEFT -> "l"
        Align.CENTER -> "c"
        Align.RIGHT -> "r"
    }

    private fun css(paper: PaperSpec): String {
        val pageSize = when (paper) {
            is PaperSpec.Page -> {
                val s = when (paper.size) {
                    PageSize.A4 -> "A4"; PageSize.A5 -> "A5"; PageSize.A6 -> "A6"; PageSize.A7 -> "A7"
                    PageSize.LETTER -> "letter"; PageSize.LEGAL -> "legal"
                }
                if (paper.orientation == Orientation.LANDSCAPE) "$s landscape" else s
            }
            else -> "A4"
        }
        return """
            @page { size: $pageSize; margin: 12mm; }
            html, body { margin: 0; padding: 0; }
            body { font-family: Arial, sans-serif; font-size: 11px; color: #000; }
            .doc { width: 100%; box-sizing: border-box; }
            p { margin: 2px 0; } .b { font-weight: bold; }
            .l { text-align: left; } .c { text-align: center; } .r { text-align: right; }
            .kv { display: flex; justify-content: space-between; }
            table { width: 100%; border-collapse: collapse; margin: 6px 0; }
            table.items { table-layout: fixed; }
            th, td { border-bottom: 1px solid #999; padding: 3px 4px; word-wrap: break-word; overflow-wrap: break-word; }
            thead { display: table-header-group; }
            tr { break-inside: avoid; }
            table.grid { table-layout: auto; }
            table.grid td { border: none; padding: 2px 8px; vertical-align: top; }
            .gl { font-weight: bold; }
            .logo { max-height: 60px; }
            hr { border: none; border-top: 1px dashed #666; }
        """.trimIndent()
    }

    private fun esc(s: String): String = buildString {
        for (ch in s) when (ch) {
            '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;")
            '"' -> append("&quot;"); else -> append(ch)
        }
    }
}
