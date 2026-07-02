package com.ampairs.product.resolver

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.data.PurchaseCostResolver
import com.ampairs.product.db.dao.ProductStandardCostDao
import com.ampairs.product.db.entity.ProductStandardCost
import com.ampairs.product.db.entity.toDomain
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Product-backed [PurchaseCostResolver] — the cost-side mirror of
 * [com.ampairs.pricing.resolver.PricingPriceResolver]. Resolves the effective standard purchase cost
 * from the local effective-dated read model: filters to the cost versions whose window contains the
 * as-of instant (effectiveFrom inclusive, effectiveTo exclusive), prefers a variant match over the
 * base product, then picks the most-recently-effective version. Returns `null` when no standard cost
 * applies so the purchase form keeps its catalog fallback (DP / selling price).
 *
 * Bound at [WorkspaceScope] (the standard-cost DB is workspace-scoped) — same pattern as
 * PricingPriceResolver; the single PurchaseCostResolver binding the workspace-scoped purchase
 * ViewModel resolves.
 */
@OptIn(ExperimentalTime::class)
@Inject
@ContributesBinding(WorkspaceScope::class)
class ProductPurchaseCostResolver(
    private val dao: ProductStandardCostDao,
) : PurchaseCostResolver {

    override suspend fun resolve(productId: String, variantSku: String?, asOfDate: String?): Double? {
        val asOf = parseInstantOrNull(asOfDate) ?: Clock.System.now()
        val items = dao.getByProductId(productId).map { it.toDomain() }
        return pickStandardCost(items, variantSku, asOf)?.costPrice
    }
}

// --- Pure helpers (testable, no IO) ---------------------------------------------------------

/**
 * Picks the standard cost effective at [asOf] for [variantSku] (falling back to the base product):
 * filters to the effective-window, then variant-matches > base-matches > any, and within that pool
 * returns the most-recently-effective version (max effectiveFrom). Returns null when none apply.
 * Mirrors `PriceListRepository.bestItemFor`.
 */
@OptIn(ExperimentalTime::class)
internal fun pickStandardCost(
    items: List<ProductStandardCost>,
    variantSku: String?,
    asOf: Instant,
): ProductStandardCost? {
    val effective = items.filter { it.isEffectiveAt(asOf) }
    if (effective.isEmpty()) return null
    val variantMatches = if (variantSku != null) effective.filter { it.variantSku == variantSku } else emptyList()
    val baseMatches = effective.filter { it.variantSku == null }
    val pool = variantMatches.ifEmpty { baseMatches.ifEmpty { effective } }
    return pool.maxByOrNull { parseInstantOrNull(it.effectiveFrom) ?: Instant.DISTANT_PAST }
}

/**
 * True when this cost's effective window contains [asOf]: from `effectiveFrom` (inclusive) up to
 * `effectiveTo` (exclusive). A null/blank bound is open on that side, so a legacy row with no
 * effective dates is always effective. Unparseable bounds are treated as open (fail-open).
 * Mirrors `PriceListItem.isEffectiveAt`.
 */
@OptIn(ExperimentalTime::class)
internal fun ProductStandardCost.isEffectiveAt(asOf: Instant): Boolean {
    parseInstantOrNull(effectiveFrom)?.let { if (asOf < it) return false }
    parseInstantOrNull(effectiveTo)?.let { if (asOf >= it) return false }
    return true
}

/** Parse an ISO-8601 instant string, or null when blank/unparseable. */
@OptIn(ExperimentalTime::class)
internal fun parseInstantOrNull(iso: String?): Instant? =
    if (iso.isNullOrBlank()) null else runCatching { Instant.parse(iso) }.getOrNull()
