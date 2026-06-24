package com.ampairs.pricing.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.PriceList

/**
 * Pricing API — the unified `/sync` contract for price lists and geo zones.
 *  - PULL via `getPriceListsSync` / `getGeoZonesSync` (GET, snake_case params, includes soft-deleted rows)
 *  - PUSH via `bulkUpdatePriceLists` / `bulkUpdateGeoZones` (POST bulk upsert, soft-deletes in-band)
 *
 * Price lists are aggregate-grained: each [PriceList] carries its `items`.
 */
interface PricingApi {

    suspend fun getPriceListsSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<PriceList>

    suspend fun bulkUpdatePriceLists(priceLists: List<PriceList>): List<PriceList>

    suspend fun getGeoZonesSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<GeoZone>

    suspend fun bulkUpdateGeoZones(geoZones: List<GeoZone>): List<GeoZone>
}
