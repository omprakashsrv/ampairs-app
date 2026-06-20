package com.ampairs.printing.core.model

import kotlinx.serialization.Serializable

/**
 * A workspace-synced, versioned definition of how a [DocumentType] renders for a [PrinterClass] and
 * paper size. Authored in the visual editor; resolved against a document by the engine at print time.
 * Stored by stable [FieldBinding.fieldKey]s (never positions) so it survives template sync (§7).
 */
@Serializable
data class Template(
    val id: String,
    val name: String,
    val documentType: DocumentType,
    val printerClass: PrinterClass,
    val paper: PaperSpec,
    val version: Long = 1,
    val blocks: List<TemplateBlock> = emptyList(),
)

/** A reference from a template element to a data field, resolved against a document at print time. */
@Serializable
data class FieldBinding(
    /** Matches feature/form's FormField.fieldKey (standard/custom) or a computed-field key. */
    val fieldKey: String,
    val scope: BindingScope = BindingScope.DOCUMENT,
    val entityRef: EntityRef = EntityRef.SELF,
    val source: FieldSource = FieldSource.STANDARD,
    /** Optional format override (date pattern / decimals / uppercase) applied by the renderer. */
    val format: String? = null,
    /** Shown when the resolved value is null/blank. */
    val fallback: String? = null,
)

@Serializable
data class TemplateStyle(
    val bold: Boolean = false,
    val align: Align = Align.LEFT,
    val widthScale: Int = 1,
    val heightScale: Int = 1,
) {
    fun toPrintStyle() = TextStyle(bold, align, widthScale, heightScale)
}

@Serializable
data class TemplateColumn(
    val header: String,
    val binding: FieldBinding,
    val weight: Int = 1,
    val align: Align = Align.LEFT,
)

/** One cell of a [TemplateBlock.InfoGrid]: a static [label] plus an optional bound value. */
@Serializable
data class GridCell(
    val label: String = "",
    val binding: FieldBinding? = null,
)

/** The authored blocks; the engine turns these into [PrintElement]s by resolving bindings. */
@Serializable
sealed interface TemplateBlock {
    val region: BandRegion

    /** A bound, single value (e.g. invoice number, customer name). */
    @Serializable
    data class BoundText(
        val binding: FieldBinding,
        val style: TemplateStyle = TemplateStyle(),
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    /** Literal text (titles, "computer-generated" note). */
    @Serializable
    data class StaticText(
        val text: String,
        val style: TemplateStyle = TemplateStyle(),
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    /** Label + bound value on one line. */
    @Serializable
    data class KeyValue(
        val label: String,
        val binding: FieldBinding,
        val style: TemplateStyle = TemplateStyle(),
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    /** Repeating line table; columns carry LINE-scope bindings resolved per item. */
    @Serializable
    data class LineTable(
        val columns: List<TemplateColumn>,
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    /**
     * A fixed grid of labelled cells (not line-scoped), laid out [columns] per row — e.g. a
     * seller/GSTIN box or a "Bill To | Ship To" header. Each cell shows a static label plus an
     * optional bound value. Renders as an HTML `<table>` on page printers (desktop-safe).
     */
    @Serializable
    data class InfoGrid(
        val cells: List<GridCell>,
        val columns: Int = 2,
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    @Serializable
    data class Divider(
        val char: Char = '-',
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    @Serializable
    data class Spacer(
        val lines: Int = 1,
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    /** Store logo, sourced from the business profile and rasterized per printer class. */
    @Serializable
    data class Logo(
        val ref: String = "business_logo",
        override val region: BandRegion = BandRegion.HEADER,
    ) : TemplateBlock

    @Serializable
    data class BarcodeField(
        val binding: FieldBinding,
        val symbology: Symbology = Symbology.CODE128,
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    /** QR from a bound value (e.g. the `upi_qr` computed field for scan-to-pay). */
    @Serializable
    data class QrField(
        val binding: FieldBinding,
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    @Serializable
    data class CutMark(
        val partial: Boolean = true,
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock

    @Serializable
    data class CashDrawer(
        override val region: BandRegion = BandRegion.BODY,
    ) : TemplateBlock
}
