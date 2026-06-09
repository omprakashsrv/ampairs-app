package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.postList
import com.ampairs.customer.domain.CustomerType
import com.ampairs.common.model.Response
import com.ampairs.common.model.PageResponse
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class CustomerTypeApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerTypeApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomerTypes(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String
    ): Response<PageResponse<CustomerType>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sort_by" to sortBy,
            "sort_dir" to sortDirection
        )

        lastSyncTime?.takeIf { it.isNotBlank() }?.let { params["last_sync"] = it }

        // Incremental sync feed: returns rows updated since last_sync INCLUDING inactive
        // (soft-deleted) types, so the client can permanently delete removed rows.
        val response: Response<PageResponse<CustomerType>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/types/sync"),
            params
        )
        return response
    }

    override suspend fun getAvailableCustomerTypesForImport(): Response<List<CustomerType>> {
        val response: Response<List<CustomerType>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/types/available")
        )
        return response
    }

    override suspend fun getCustomerTypeById(id: String): Response<CustomerType> {
        val response: Response<CustomerType> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/types/$id")
        )
        return response
    }

    override suspend fun bulkUpsertTypes(types: List<CustomerType>): Result<List<CustomerType>> = runCatching {
        val response: Response<List<CustomerType>> = postList(client, ApiUrlBuilder.customerUrl("v1/types/sync"), types)
        response.data ?: emptyList()
    }

    override suspend fun searchCustomerTypes(
        query: String,
        page: Int,
        size: Int
    ): Response<PageResponse<CustomerType>> {
        val params = mapOf(
            "query" to query,
            "page" to page.toString(),
            "size" to size.toString()
        )
        val response: Response<PageResponse<CustomerType>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/types/search"),
            params
        )
        return response
    }
}