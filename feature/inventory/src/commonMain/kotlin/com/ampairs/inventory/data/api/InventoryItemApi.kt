package com.ampairs.inventory.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.inventory.domain.InventoryItem

interface InventoryItemApi {

    suspend fun getItems(
        page: Int = 0,
        size: Int = 100,
        lastSyncTime: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: String = "ASC",
    ): Response<PageResponse<InventoryItem>>

    suspend fun bulkUpsertItems(items: List<InventoryItem>): Result<List<InventoryItem>>
}
