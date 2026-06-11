package com.ampairs.invoice.editor

import com.ampairs.tax.calculation.document.DiscountKind

/**
 * Neutral UI models for the shared order/invoice "fast entry" editor (spec 010 v2 design).
 * Both ViewModels map their domain items (OrderItem / InvoiceItem) into these so every editor
 * surface — command bar, line grid, line cards, totals rail — is implemented once.
 */

/** Sync chip states (design: saved-offline → syncing → synced; failed shows inline retry). */
enum class DocSyncUi { NONE, OFFLINE, SYNCING, SYNCED, FAILED }

/** The buyer shown in the header chip and the invoice Bill-to block. */
data class DocCustomerUi(
    val id: String,
    val name: String,
    val initials: String,
    /** e.g. "Maharashtra (27)"; null when the state isn't known. */
    val stateLabel: String?,
    val intra: Boolean,
    val walkIn: Boolean,
)

/** One document line, fully computed (taxable/GST/total come from calc-core). */
data class DocLineUi(
    val id: String,
    val name: String,
    val hsn: String,
    /** Total GST percent for the line's tax code in the current scenario; null/0 → exempt. */
    val gstRatePercent: Double?,
    val hasVariants: Boolean,
    val variantSku: String?,
    val unitName: String,
    val unitMultiplier: Double,
    val baseUnitName: String,
    val decimalPlaces: Int,
    val quantity: Double,
    val baseQuantity: Double,
    val unitPrice: Double,
    val priceOverridden: Boolean,
    /** Catalog price for the current unit/variant — shown when the price is overridden. */
    val catalogUnitPrice: Double,
    val discountKind: DiscountKind,
    val discountAmount: Double,
    val taxable: Double,
    val totalTax: Double,
    val lineTotal: Double,
) {
    val exempt: Boolean get() = (gstRatePercent ?: 0.0) <= 0.0
}

/** A result row under the command bar: full visual confirmation before commit. */
data class ComposerResultUi(
    val productId: String,
    val name: String,
    val hsn: String,
    val gstRatePercent: Double?,
    val barcode: String?,
    val quantity: Double,
    val unitName: String,
    val unitPrice: Double,
    val priceOverridden: Boolean,
    val discountPercent: Double,
    val amount: Double,
)

/** Everything the command bar renders; rebuilt by the ViewModel on each keystroke. */
data class ComposerUiState(
    val query: String = "",
    val results: List<ComposerResultUi> = emptyList(),
    val highlight: Int = 0,
    /** Words typed (joined) — pre-fills inline create when nothing matches. */
    val typedWords: String = "",
    val noMatch: Boolean = false,
    /** Parse chips for the highlighted result: qty/unit, price, −disc%, = amount. */
    val preview: ComposerResultUi? = null,
    /** Transient "Added <product> · <qty> <unit>" confirmation (cleared after ~1.4 s). */
    val flash: String? = null,
)

/** Unit choice row for the unit picker: label, ×multiplier and the rescaled price. */
data class DocUnitChoiceUi(
    val unitId: String,
    val name: String,
    val multiplier: Double,
    val priceAtUnit: Double,
    val decimalPlaces: Int,
    val isBase: Boolean,
)

/** Variant choice row: label + the price that will apply. */
data class DocVariantChoiceUi(
    val sku: String,
    val label: String,
    val price: Double,
)
