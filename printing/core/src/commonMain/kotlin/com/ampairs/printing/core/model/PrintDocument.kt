package com.ampairs.printing.core.model

/**
 * Printer-agnostic intermediate representation produced by [com.ampairs.printing.core.engine.PrintEngine]
 * and consumed by every renderer. Values are typed [FieldValue]s; renderers format them with a
 * [ValueFormatter]. This is the single decoupling point between "what to print" and "how to print".
 */
data class PrintDocument(
    val meta: PrintMeta,
    val blocks: List<PrintElement>,
)

data class PrintMeta(
    val documentType: DocumentType,
    val copies: Int = 1,
    /** e.g. "ORIGINAL FOR RECIPIENT" / "DUPLICATE" (GST copy label) — null when not applicable. */
    val copyLabel: String? = null,
    val isDuplicate: Boolean = false,
)

data class TextStyle(
    val bold: Boolean = false,
    val align: Align = Align.LEFT,
    val widthScale: Int = 1,
    val heightScale: Int = 1,
)

/** Visual style for a table/text column. */
data class ColumnStyle(
    val weight: Int = 1,
    val align: Align = Align.LEFT,
)

sealed interface PrintElement {
    /** Page-mode band; thermal/label renderers ignore it and flow top-to-bottom. */
    val region: BandRegion

    data class TextLine(
        val content: FieldValue,
        val style: TextStyle = TextStyle(),
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class KeyValueRow(
        val label: FieldValue,
        val value: FieldValue,
        val style: TextStyle = TextStyle(),
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class Table(
        val columns: List<TableHeader>,
        val rows: List<List<FieldValue>>,
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    /**
     * A fixed (non-line) grid of labelled values laid out in [columns] cells per row — e.g. a
     * seller/GSTIN box or a "Bill To | Ship To" two-panel header. Page renderers lay it out as an
     * HTML `<table>` (desktop JEditorPane has no CSS grid); thermal flows it as label/value lines.
     */
    data class Grid(
        val cells: List<GridCellValue>,
        val columns: Int = 2,
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class Divider(
        val char: Char = '-',
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class Spacer(
        val lines: Int = 1,
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class Image(
        /** Logo reference (cache key / url / data id) resolved by the renderer. */
        val ref: String,
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class Barcode(
        val value: String,
        val symbology: Symbology = Symbology.CODE128,
        val heightDots: Int = 60,
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class Qr(
        val value: String,
        val sizeModule: Int = 6,
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class Feed(
        val lines: Int = 1,
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class Cut(
        val partial: Boolean = true,
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement

    data class CashDrawerKick(
        override val region: BandRegion = BandRegion.BODY,
    ) : PrintElement
}

data class TableHeader(
    val title: String,
    val style: ColumnStyle = ColumnStyle(),
)

/** One resolved cell of a [PrintElement.Grid]: a static [label] plus its resolved [value]. */
data class GridCellValue(
    val label: String,
    val value: FieldValue,
)
