package com.ampairs.inventory.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.postList
import com.ampairs.inventory.domain.InventoryItem
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class InventoryItemApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : InventoryItemApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getItems(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String,
    ): Response<PageResponse<InventoryItem>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sort_by" to sortBy,
            "sort_dir" to sortDirection,
        )
        lastSyncTime?.takeIf { it.isNotBlank() }?.let { params["last_sync"] = it }
        return get(client, ApiUrlBuilder.inventoryUrl("v1/items/sync"), params)
    }

    override suspend fun bulkUpsertItems(items: List<InventoryItem>): Result<List<InventoryItem>> = runCatching {
        val response: Response<List<InventoryItem>> =
            postList(client, ApiUrlBuilder.inventoryUrl("v1/items/sync"), items)
        response.data ?: emptyList()
    }
}
