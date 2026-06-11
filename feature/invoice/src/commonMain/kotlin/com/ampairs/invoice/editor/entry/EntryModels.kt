package com.ampairs.invoice.editor.entry

import com.ampairs.product.domain.ProductSummary

/**
 * Pure models for the command-bar fast-entry composer (spec 010 v2 design, screen "composer").
 * Shared by the order and invoice editors. No Compose / DI / IO here — the ViewModel supplies
 * candidates (catalog rows + their sellable units) and consumes [EntryMatch]/[EntryPreview].
 */

/** The structured interpretation of the typed query, e.g. `parle 5 box 10% @2000`. */
data class ParsedEntry(
    /** First bare number in the query (quantity), or null. */
    val quantity: Double? = null,
    /** `N%` token → line discount percent. */
    val discountPercent: Double? = null,
    /** `@N` / `₹N` token → unit-price override. */
    val priceOverride: Double? = null,
    /** Remaining lowercase words: product-name words, HSN prefixes, barcode-like digit runs, unit names. */
    val words: List<String> = emptyList(),
) {
    val isBlank: Boolean get() = quantity == null && discountPercent == null && priceOverride == null && words.isEmpty()
}

/** A sellable unit of a candidate product (base first; multiplier vs the base unit). */
data class EntryUnit(
    val unitId: String,
    val name: String,
    val multiplier: Double,
    val decimalPlaces: Int,
    val isBase: Boolean,
)

/** One catalog candidate offered to the matcher: the product plus its sellable units. */
data class EntryCandidate(
    val product: ProductSummary,
    val units: List<EntryUnit>,
) {
    /** Barcode surrogate: the product code when it is an 8+ digit run (USB scanners emit digits). */
    val barcode: String? get() = product.code.takeIf { it.length >= 8 && it.all(Char::isDigit) }
}

/** A candidate that survived matching, with the unit selected by a unit-name word (if any). */
data class EntryMatch(
    val candidate: EntryCandidate,
    val unit: EntryUnit?,
    val nameHits: Int,
    val exactBarcode: Boolean,
    val isRecent: Boolean,
)

/** A fully resolved line preview for a match + parse, ready to commit or render as a result row. */
data class EntryPreview(
    val product: ProductSummary,
    val unit: EntryUnit,
    val quantity: Double,
    val unitPrice: Double,
    val priceOverridden: Boolean,
    val discountPercent: Double,
    /** price × qty × (1 − disc%) rounded to 2 decimals — the pre-tax line amount. */
    val amount: Double,
)
