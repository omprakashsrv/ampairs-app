package com.ampairs.printing.render.html

import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.Orientation
import com.ampairs.printing.core.model.PageSize
import com.ampairs.printing.core.model.PaperSpec
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
                // Legacy align attr + <b>/<font> tags — desktop JEditorPane (HTML 3.2) ignores
                // CSS class selectors, so style must travel as attributes/tags to render everywhere.
                sb.append("<p align=\"").append(htmlAlign(element.style.align)).append("\">")
                appendStyled(sb, formatter.format(element.content), element.style.bold, scaleOf(element.style))
                sb.append("</p>")
            }

            is PrintElement.KeyValueRow -> {
                // Label-left / value-right via a 2-cell table (JEditorPane has no flexbox).
                val scale = scaleOf(element.style)
                sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr><td align=\"left\">")
                appendStyled(sb, formatter.format(element.label), element.style.bold, scale)
                sb.append("</td><td align=\"right\">")
                appendStyled(sb, formatter.format(element.value), element.style.bold, scale)
                sb.append("</td></tr></table>")
            }

            is PrintElement.Table -> {
                // Size columns by weight (width attr, supported in JEditorPane + WebViews) so narrow
                // numeric columns (Qty/Rate/Amount) stay visible instead of being squeezed out.
                val total = element.columns.sumOf { it.style.weight }.coerceAtLeast(1)
                sb.append("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"3\"><thead><tr>")
                for (c in element.columns) {
                    sb.append("<th width=\"").append(c.style.weight * 100 / total)
                        .append("%\" align=\"").append(htmlAlign(c.style.align)).append("\">")
                        .append(esc(c.title)).append("</th>")
                }
                sb.append("</tr></thead><tbody>")
                for (row in element.rows) {
                    sb.append("<tr>")
                    row.forEachIndexed { i, v ->
                        val c = element.columns.getOrNull(i)
                        val w = if (c != null) c.style.weight * 100 / total else 0
                        val a = c?.style?.align ?: Align.LEFT
                        sb.append("<td width=\"").append(w).append("%\" align=\"").append(htmlAlign(a))
                            .append("\">").append(esc(formatter.format(v))).append("</td>")
                    }
                    sb.append("</tr>")
                }
                sb.append("</tbody></table>")
            }

            is PrintElement.Grid -> {
                val cols = element.columns.coerceAtLeast(1)
                val w = 100 / cols
                sb.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"2\"><tbody>")
                element.cells.chunked(cols).forEach { rowCells ->
                    sb.append("<tr>")
                    for (cell in rowCells) {
                        sb.append("<td width=\"").append(w).append("%\" valign=\"top\">")
                        if (cell.label.isNotBlank()) {
                            sb.append("<b>").append(esc(cell.label)).append("</b> ")
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
                    sb.append("<img src=\"").append(esc(ref)).append("\" height=\"60\">")
                }
            }
            is PrintElement.Barcode -> sb.append("<p align=\"center\">").append(esc(element.value)).append("</p>")
            is PrintElement.Qr -> sb.append("<p align=\"center\">").append(esc(element.value)).append("</p>")
            is PrintElement.Cut -> Unit          // page printers do not cut
            is PrintElement.CashDrawerKick -> Unit
        }
    }

    /** Wrap [text] in `<font size>`/`<b>` so size + bold render in JEditorPane and WebViews alike. */
    private fun appendStyled(sb: StringBuilder, text: String, bold: Boolean, scale: Int) {
        if (scale > 1) sb.append("<font size=\"6\">")
        if (bold) sb.append("<b>")
        sb.append(esc(text))
        if (bold) sb.append("</b>")
        if (scale > 1) sb.append("</font>")
    }

    /** Print scale (1 = normal, >1 = "Large") from the block style's width/height multipliers. */
    private fun scaleOf(style: com.ampairs.printing.core.model.TextStyle): Int =
        maxOf(style.widthScale, style.heightScale)

    private fun htmlAlign(align: Align): String = when (align) {
        Align.LEFT -> "left"
        Align.CENTER -> "center"
        Align.RIGHT -> "right"
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
        // Styling travels as HTML attributes/tags (align=, <b>, <font>, td width=) so it renders in
        // JEditorPane too; this CSS only carries print page setup + a few enhancements WebViews honor.
        return """
            @page { size: $pageSize; margin: 12mm; }
            html, body { margin: 0; padding: 0; }
            body { font-family: Arial, sans-serif; font-size: 12px; color: #000; }
            p { margin: 2px 0; }
            table { border-collapse: collapse; }
            td, th { word-wrap: break-word; overflow-wrap: break-word; vertical-align: top; }
            thead { display: table-header-group; }
            tr { break-inside: avoid; }
            img { max-height: 60px; }
            hr { border: none; border-top: 1px solid #666; }
        """.trimIndent()
    }

    private fun esc(s: String): String = buildString {
        for (ch in s) when (ch) {
            '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;")
            '"' -> append("&quot;"); else -> append(ch)
        }
    }
}
