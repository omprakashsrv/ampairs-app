package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.postList
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class CustomerGroupApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerGroupApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomerGroups(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String
    ): Response<PageResponse<CustomerGroup>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sortBy" to sortBy,
            "sortDirection" to sortDirection
        )

        lastSyncTime?.let { params["last_sync"] = it }

        // Incremental sync feed: returns rows updated since last_sync INCLUDING inactive
        // (soft-deleted) groups, so the client can permanently delete removed rows.
        val response: Response<PageResponse<CustomerGroup>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/groups/sync"),
            params
        )
        return response
    }

    override suspend fun getAvailableCustomerGroupsForImport(): Response<List<CustomerGroup>> {
        val response: Response<List<CustomerGroup>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/groups/available")
        )
        return response
    }

    override suspend fun bulkUpsertGroups(groups: List<CustomerGroup>): Result<List<CustomerGroup>> = runCatching {
        val response: Response<List<CustomerGroup>> = postList(client, ApiUrlBuilder.customerUrl("v1/groups"), groups)
        response.data ?: emptyList()
    }

    override suspend fun deleteCustomerGroup(id: String): Response<Unit> {
        val response: Response<Unit> = delete(
            client,
            ApiUrlBuilder.customerUrl("v1/groups/$id")
        )
        return response
    }
}