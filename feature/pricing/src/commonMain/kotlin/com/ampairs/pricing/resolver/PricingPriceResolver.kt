package com.ampairs.pricing.resolver

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.SalesChannel
import com.ampairs.product.data.PriceResolver
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Pricing-backed [PriceResolver] (spec 009 US3, minimal). Resolves the effective unit price from the
 * local price-list read model for the RETAIL channel — product-level lists + quantity tiers — and
 * falls back to the catalog [fallbackUnitPrice] when no list matches (CATALOG_FALLBACK).
 *
 * Bound at [WorkspaceScope] (the price-list DB is workspace-scoped), the same pattern as
 * TaxRateProvider/InventoryDataService; it is the single PriceResolver binding the workspace-scoped
 * order/invoice ViewModels resolve. Customer-group / wholesale / geo-zone context is not threaded yet
 * (the seam carries none) — that's a follow-up that enriches the port.
 */
@Inject
@ContributesBinding(WorkspaceScope::class)
class PricingPriceResolver(
    private val repository: PriceListRepository,
) : PriceResolver {
    override suspend fun resolveUnitPrice(
        productId: String,
        variantSku: String?,
        quantity: Double,
        fallbackUnitPrice: Double,
    ): Double = repository.resolve(
        channel = SalesChannel.RETAIL,
        productId = productId,
        quantity = quantity,
        fallbackUnitPrice = fallbackUnitPrice,
        variantSku = variantSku,
    ).effectiveUnitPrice
}
