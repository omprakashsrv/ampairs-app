package com.ampairs.cbstore.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.cbstore.domain.model.ZonalOffice
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CbStoreApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : CbStoreApi {

    private val client = httpClient(engine, tokenRepository)

    private fun syncParams(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String) =
        mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        ).also { if (lastSync.isNotBlank()) it["last_sync"] = lastSync }

    override suspend fun getStoresSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<Store> {
        val response: Response<PageResponse<Store>> = get(
            client,
            ApiUrlBuilder.cbStoreUrl("v1/stores/sync"),
            syncParams(lastSync, page, size, sortBy, sortDir),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateStores(stores: List<Store>): List<Store> {
        val response: Response<List<Store>> = post(client, ApiUrlBuilder.cbStoreUrl("v1/stores/sync"), stores)
        return response.data ?: throw Exception("Failed to bulk update stores")
    }

    override suspend fun getStoreById(id: String): Response<Store> =
        get(client, ApiUrlBuilder.cbStoreUrl("v1/stores/$id"))

    override suspend fun getZonalOfficesSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<ZonalOffice> {
        val response: Response<PageResponse<ZonalOffice>> = get(
            client,
            ApiUrlBuilder.cbStoreUrl("v1/zonal-offices/sync"),
            syncParams(lastSync, page, size, sortBy, sortDir),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateZonalOffices(offices: List<ZonalOffice>): List<ZonalOffice> {
        val response: Response<List<ZonalOffice>> =
            post(client, ApiUrlBuilder.cbStoreUrl("v1/zonal-offices/sync"), offices)
        return response.data ?: throw Exception("Failed to bulk update zonal offices")
    }

    override suspend fun getZonalOfficeById(id: String): Response<ZonalOffice> =
        get(client, ApiUrlBuilder.cbStoreUrl("v1/zonal-offices/$id"))

    private fun <T> emptyPage(page: Int, size: Int): PageResponse<T> = PageResponse(
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
}
