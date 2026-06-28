package com.ampairs.product.data

/**
 * Context for resolving an effective unit price. Carries the product/variant + quantity plus the
 * buyer/channel context so a pricing-aware resolver can apply channel, customer-group, customer,
 * and geo-zone price lists. [channel] is a plain string ("RETAIL" / "WHOLESALE") to keep this port
 * free of any pricing-module type (product-api must not depend on feature:pricing).
 */
data class PriceResolutionInput(
    val productId: String,
    val variantSku: String? = null,
    val quantity: Double,
    val fallbackUnitPrice: Double,
    val channel: String = "RETAIL",
    val customerId: String? = null,
    val customerGroupId: String? = null,
    val customerType: String? = null,
    val pincode: String? = null,
    /**
     * The document date the price is resolved "as of" — the order/invoice date for billing (Tally's
     * "Applicable From"). The resolver picks the price version effective at this instant. ISO-8601
     * (UTC); null => now. Plain String to keep this port free of any date-library type.
     */
    val asOfDate: String? = null,
)

/**
 * Outcome of a price resolution (spec 009 US3). [unitPrice] is the major-unit price the caller bills
 * with; the remaining fields are the resolution snapshot persisted verbatim onto the order/invoice
 * line and pushed on `/sync` for audit (the backend never re-resolves). All fields are plain types so
 * this port stays free of any pricing-module dependency.
 */
data class ResolvedPrice(
    val unitPrice: Double,
    val resolvedUnitPriceMinor: Long? = null,
    val currency: String? = null,
    val priceSource: String? = null,
    val matchedPriceListUid: String? = null,
    val appliedTierMinQty: Double? = null,
    val belowMoq: Boolean = false,
)

/**
 * Client-side price-resolution seam (spec 009 / 010). The merchant order/invoice line build resolves
 * the effective unit price through this port instead of reading `product.sellingPrice` directly.
 *
 * The pricing-aware implementation (feature:pricing) applies the local price-list read model; when no
 * list matches it returns [PriceResolutionInput.fallbackUnitPrice] as a CATALOG_FALLBACK. Resolution
 * is local/offline.
 */
interface PriceResolver {
    suspend fun resolve(input: PriceResolutionInput): ResolvedPrice
}
