package com.ampairs.pricing.resolver

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.SalesChannel
import com.ampairs.product.data.PriceResolutionInput
import com.ampairs.product.data.PriceResolver
import com.ampairs.product.data.ResolvedPrice
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.math.roundToLong

/**
 * Pricing-backed [PriceResolver] (spec 009 US3). Resolves the effective unit price from the local
 * price-list read model, honoring channel + customer-group/type/customer + delivery pincode
 * (geo-zone), with quantity tiers; falls back to the catalog price when no list matches.
 *
 * Bound at [WorkspaceScope] (the price-list DB is workspace-scoped) — same pattern as
 * TaxRateProvider/InventoryDataService; the single PriceResolver binding the workspace-scoped
 * order/invoice ViewModels resolve.
 */
@Inject
@ContributesBinding(WorkspaceScope::class)
class PricingPriceResolver(
    private val repository: PriceListRepository,
) : PriceResolver {
    override suspend fun resolve(input: PriceResolutionInput): ResolvedPrice {
        val resolution = repository.resolve(
            channel = if (input.channel.equals("WHOLESALE", ignoreCase = true)) SalesChannel.WHOLESALE else SalesChannel.RETAIL,
            productId = input.productId,
            quantity = input.quantity,
            fallbackUnitPrice = input.fallbackUnitPrice,
            variantSku = input.variantSku,
            customerId = input.customerId,
            customerGroupId = input.customerGroupId,
            customerType = input.customerType,
            pincode = input.pincode,
        )
        return ResolvedPrice(
            unitPrice = resolution.effectiveUnitPrice,
            // Minor units (2 dp) — what the backend snapshot column stores; mirrors MoneyDto.amount_minor.
            resolvedUnitPriceMinor = (resolution.effectiveUnitPrice * 100).roundToLong(),
            currency = resolution.currency,
            priceSource = resolution.source.name,
            matchedPriceListUid = resolution.matchedPriceListUid,
            appliedTierMinQty = resolution.appliedTierMinQty,
            belowMoq = resolution.belowMoq,
        )
    }
}
