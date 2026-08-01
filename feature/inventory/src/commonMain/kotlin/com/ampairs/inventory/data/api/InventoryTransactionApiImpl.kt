package com.ampairs.inventory.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.postList
import com.ampairs.inventory.domain.InventoryMovement
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class InventoryTransactionApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : InventoryTransactionApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getMovements(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String,
    ): Response<PageResponse<InventoryMovement>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sort_by" to sortBy,
            "sort_dir" to sortDirection,
        )
        lastSyncTime?.takeIf { it.isNotBlank() }?.let { params["last_sync"] = it }
        return get(client, ApiUrlBuilder.inventoryUrl("v1/transactions/sync"), params)
    }

    override suspend fun bulkUpsertMovements(
        movements: List<InventoryMovement>,
    ): Result<List<InventoryMovement>> = runCatching {
        val response: Response<List<InventoryMovement>> =
            postList(client, ApiUrlBuilder.inventoryUrl("v1/transactions/sync"), movements)
        response.data ?: emptyList()
    }
}
