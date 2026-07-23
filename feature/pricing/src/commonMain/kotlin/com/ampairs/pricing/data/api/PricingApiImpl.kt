package com.ampairs.pricing.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.PriceList
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

/**
 * Ktor implementation of [PricingApi] against `/api/pricing/v1/price-lists/sync`,
 * `/api/pricing/v1/price-lists/items/sync`, and `/api/pricing/v1/geo-zones/sync`.
 */
@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class PricingApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : PricingApi {

    private val client = httpClient(engine, tokenRepository)

    private fun <T> emptyPage(page: Int, size: Int) = PageResponse<T>(
        content = emptyList(),
        pageNumber = page,
        pageSize = size,
        totalPages = 0,
        totalElements = 0L,
        hasNext = false,
        hasPrevious = false,
        first = true,
        last = true,
    )

    private fun syncParams(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): Map<String, Any> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) params["last_sync"] = lastSync
        return params
    }

    override suspend fun getPriceListsSync(
        lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String,
    ): PageResponse<PriceList> {
        val response: Response<PageResponse<PriceList>> = get(
            client, ApiUrlBuilder.pricingUrl("v1/price-lists/sync"),
            syncParams(lastSync, page, size, sortBy, sortDir),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdatePriceLists(priceLists: List<PriceList>): List<PriceList> {
        val response: Response<List<PriceList>> = post(
            client, ApiUrlBuilder.pricingUrl("v1/price-lists/sync"), priceLists,
        )
        return response.data ?: throw Exception("Failed to bulk update price lists")
    }

    override suspend fun getPriceListItemsSync(
        lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String,
    ): PageResponse<PriceListItemResponse> {
        val response: Response<PageResponse<PriceListItemResponse>> = get(
            client, ApiUrlBuilder.pricingUrl("v1/price-lists/items/sync"),
            syncParams(lastSync, page, size, sortBy, sortDir),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdatePriceListItems(items: List<PriceListItemPush>): List<PriceListItemResponse> {
        val response: Response<List<PriceListItemResponse>> = post(
            client, ApiUrlBuilder.pricingUrl("v1/price-lists/items/sync"), items,
        )
        return response.data ?: throw Exception("Failed to bulk update price list items")
    }

    override suspend fun getGeoZonesSync(
        lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String,
    ): PageResponse<GeoZone> {
        val response: Response<PageResponse<GeoZone>> = get(
            client, ApiUrlBuilder.pricingUrl("v1/geo-zones/sync"),
            syncParams(lastSync, page, size, sortBy, sortDir),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateGeoZones(geoZones: List<GeoZone>): List<GeoZone> {
        val response: Response<List<GeoZone>> = post(
            client, ApiUrlBuilder.pricingUrl("v1/geo-zones/sync"), geoZones,
        )
        return response.data ?: throw Exception("Failed to bulk update geo zones")
    }

    override suspend fun getOffersSync(
        lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String,
    ): PageResponse<Offer> {
        val response: Response<PageResponse<Offer>> = get(
            client, ApiUrlBuilder.pricingUrl("v1/offers/sync"),
            syncParams(lastSync, page, size, sortBy, sortDir),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateOffers(offers: List<Offer>): List<Offer> {
        val response: Response<List<Offer>> = post(
            client, ApiUrlBuilder.pricingUrl("v1/offers/sync"), offers,
        )
        return response.data ?: throw Exception("Failed to bulk update offers")
    }
}
