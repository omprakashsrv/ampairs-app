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
)

/**
 * Client-side price-resolution seam (spec 009 / 010). The merchant order/invoice line build resolves
 * the effective unit price through this port instead of reading `product.sellingPrice` directly.
 *
 * The pricing-aware implementation (feature:pricing) applies the local price-list read model; when no
 * list matches it returns [PriceResolutionInput.fallbackUnitPrice]. Resolution is local/offline.
 */
interface PriceResolver {
    suspend fun resolveUnitPrice(input: PriceResolutionInput): Double
}
