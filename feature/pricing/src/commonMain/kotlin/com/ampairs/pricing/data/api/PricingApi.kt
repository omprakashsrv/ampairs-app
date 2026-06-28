package com.ampairs.pricing.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.PriceList

/**
 * Pricing API — the unified `/sync` contract. Price lists use a TWO-FEED model (mirrors backend):
 * headers via `/price-lists/sync` and items via `/price-lists/items/sync` (separate resources).
 * Geo zones sync via `/geo-zones/sync`.
 */
interface PricingApi {

    // ── price-list headers ──────────────────────────────────────────────────────────────
    suspend fun getPriceListsSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<PriceList>

    suspend fun bulkUpdatePriceLists(priceLists: List<PriceList>): List<PriceList>

    // ── price-list items (asymmetric money: pull MoneyDto / push minor) ───────────────────
    suspend fun getPriceListItemsSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<PriceListItemResponse>

    suspend fun bulkUpdatePriceListItems(items: List<PriceListItemPush>): List<PriceListItemResponse>

    // ── geo zones ─────────────────────────────────────────────────────────────────────────
    suspend fun getGeoZonesSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<GeoZone>

    suspend fun bulkUpdateGeoZones(geoZones: List<GeoZone>): List<GeoZone>

    // ── offers / promotions ───────────────────────────────────────────────────────────────
    suspend fun getOffersSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<Offer>

    suspend fun bulkUpdateOffers(offers: List<Offer>): List<Offer>
}
