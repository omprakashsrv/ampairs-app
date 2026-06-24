package com.ampairs.product.data

/**
 * Client-side price-resolution seam (spec 009 / 010). The merchant order/invoice line build resolves
 * the effective unit price through this port instead of reading `product.sellingPrice` directly.
 *
 * The default ([com.ampairs.product] passthrough) returns [fallbackUnitPrice] unchanged — no behavior
 * change. A pricing-aware implementation (feature/pricing) may apply channel/customer price lists and
 * quantity tiers. Resolution is local/offline; no backend call on the hot path.
 */
interface PriceResolver {
    suspend fun resolveUnitPrice(
        productId: String,
        variantSku: String?,
        quantity: Double,
        fallbackUnitPrice: Double,
    ): Double
}
