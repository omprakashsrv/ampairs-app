package com.ampairs.product.pricing

import com.ampairs.common.di.AppScope
import com.ampairs.product.data.PriceResolver
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Default [PriceResolver] — returns the catalog [fallbackUnitPrice] unchanged (no behavior change).
 * This is the seam the order/invoice line build resolves through; feature/pricing can later provide
 * a higher-priority binding that applies price lists + quantity tiers.
 */
@Inject
@ContributesBinding(AppScope::class)
class PassThroughPriceResolver : PriceResolver {
    override suspend fun resolveUnitPrice(
        productId: String,
        variantSku: String?,
        quantity: Double,
        fallbackUnitPrice: Double,
    ): Double = fallbackUnitPrice
}
