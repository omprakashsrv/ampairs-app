package com.ampairs.product.data

/**
 * Client-side purchase-cost resolution seam — the cost-side mirror of [PriceResolver] (sales pricing).
 * The purchase voucher line build resolves the effective standard purchase cost through this port
 * instead of reading `product.dp` / `product.sellingPrice` directly.
 *
 * The product-backed implementation (feature:product) applies the local effective-dated
 * standard-cost read model: it filters to the cost versions whose effective window contains
 * [asOfDate] (effectiveFrom inclusive, effectiveTo exclusive; a null bound is open), prefers a
 * variant match over the base product, then picks the most-recently-effective version. When no
 * standard cost applies it returns `null` so the caller keeps its catalog fallback (DP / selling
 * price). Resolution is local/offline.
 *
 * All parameters are plain types so this port stays free of any product-module dependency
 * (feature:purchase depends only on feature:product-api, never on feature:product).
 */
interface PurchaseCostResolver {
    /**
     * @param productId the catalog product id the cost is resolved for.
     * @param variantSku optional variant SKU — a variant-specific cost outranks the base cost when set.
     * @param asOfDate the document date the cost is resolved "as of" — the purchase voucher date.
     *   The resolver picks the cost version effective at this instant. ISO-8601 (UTC); null/blank => now.
     *   Plain String to keep this port free of any date-library type.
     * @return the major-unit cost price, or `null` when no standard cost applies (caller falls back).
     */
    suspend fun resolve(productId: String, variantSku: String?, asOfDate: String?): Double?
}
