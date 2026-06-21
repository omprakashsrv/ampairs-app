package com.ampairs.printing.render.thermal

import com.ampairs.printing.core.model.Align

/** A column's resolved text + how it should occupy its share of the character grid. */
data class ColumnText(
    val text: String,
    val weight: Int = 1,
    val align: Align = Align.LEFT,
    val ellipsis: Boolean = false,
)

/**
 * Pure character-grid line composition for thermal printers (the §8 alignment algorithm). No IO,
 * no ESC/POS bytes — just text → aligned monospace lines — so it is fully golden-testable.
 */
object ThermalLineComposer {

    /** Word-wrap [text] to [width] columns; never returns an empty list. */
    fun wrap(text: String, width: Int): List<String> {
        if (width <= 0) return listOf("")
        if (text.isEmpty()) return listOf("")
        val lines = ArrayList<String>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isEmpty()) {
                lines += ""
                continue
            }
            var line = StringBuilder()
            for (word in paragraph.split(" ")) {
                val piece = if (word.length > width) word else word
                when {
                    line.isEmpty() && piece.length <= width -> line.append(piece)
                    line.isEmpty() -> {
                        // Single word longer than the column — hard-split it.
                        var rest = piece
                        while (rest.length > width) {
                            lines += rest.substring(0, width)
                            rest = rest.substring(width)
                        }
                        line.append(rest)
                    }
                    line.length + 1 + piece.length <= width -> line.append(' ').append(piece)
                    else -> {
                        lines += line.toString()
                        line = StringBuilder()
                        if (piece.length <= width) {
                            line.append(piece)
                        } else {
                            var rest = piece
                            while (rest.length > width) {
                                lines += rest.substring(0, width)
                                rest = rest.substring(width)
                            }
                            line.append(rest)
                        }
                    }
                }
            }
            lines += line.toString()
        }
        return if (lines.isEmpty()) listOf("") else lines
    }

    /** Pad/align a single already-fit segment to exactly [width] characters. */
    fun pad(text: String, width: Int, align: Align): String {
        if (width <= 0) return ""
        val t = if (text.length > width) text.substring(0, width) else text
        val space = width - t.length
        return when (align) {
            Align.LEFT -> t + " ".repeat(space)
            Align.RIGHT -> " ".repeat(space) + t
            Align.CENTER -> {
                val left = space / 2
                " ".repeat(left) + t + " ".repeat(space - left)
            }
        }
    }

    /** Render a single full-width line for one cell (wrap + align), used by [FullLine]-style blocks. */
    fun line(text: String, totalWidth: Int, align: Align): List<String> =
        wrap(text, totalWidth).map { pad(it, totalWidth, align) }

    /**
     * Compose a multi-column row into [totalWidth]-wide lines. Column widths come from [ColumnText.weight]
     * (after a 1-space gap between columns). Each cell is wrapped to its width; the row emits
     * max(cell line counts) lines, shorter cells padded with blanks. The result is always exactly
     * [totalWidth] characters wide per line — alignment done in software for vendor-independence.
     */
    fun composeRow(cells: List<ColumnText>, totalWidth: Int): List<String> {
        if (cells.isEmpty() || totalWidth <= 0) return listOf("")
        val gaps = cells.size - 1
        val usable = (totalWidth - gaps).coerceAtLeast(cells.size) // at least 1 col per cell
        val widths = distribute(usable, cells.map { it.weight })

        val wrapped = cells.mapIndexed { i, c ->
            val w = widths[i]
            val raw = if (c.ellipsis) listOf(ellipsize(c.text, w)) else wrap(c.text, w)
            raw.map { pad(it, w, c.align) }
        }
        val rowLines = wrapped.maxOf { it.size }
        val out = ArrayList<String>(rowLines)
        for (k in 0 until rowLines) {
            val sb = StringBuilder()
            for (i in cells.indices) {
                if (i > 0) sb.append(' ')
                sb.append(wrapped[i].getOrElse(k) { " ".repeat(widths[i]) })
            }
            out += sb.toString()
        }
        return out
    }

    private fun ellipsize(text: String, width: Int): String = when {
        width <= 0 -> ""
        text.length <= width -> text
        width <= 1 -> text.substring(0, width)
        else -> text.substring(0, width - 1) + "…"
    }

    /** Split [total] across columns proportionally to [weights]; remainder to the widest columns. */
    private fun distribute(total: Int, weights: List<Int>): IntArray {
        val sum = weights.sum().coerceAtLeast(1)
        val widths = IntArray(weights.size) { (total * weights[it]) / sum }
        var assigned = widths.sum()
        // Hand out the rounding remainder, and guarantee every column is at least 1 wide.
        for (i in widths.indices) if (widths[i] < 1 && total >= weights.size) { widths[i] = 1; assigned++ }
        var idx = 0
        while (assigned < total && widths.isNotEmpty()) {
            widths[idx % widths.size]++
            assigned++
            idx++
        }
        return widths
    }
}
