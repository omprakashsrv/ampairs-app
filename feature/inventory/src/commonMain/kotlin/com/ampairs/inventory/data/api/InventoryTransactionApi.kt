package com.ampairs.inventory.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.inventory.domain.InventoryMovement

interface InventoryTransactionApi {

    suspend fun getMovements(
        page: Int = 0,
        size: Int = 100,
        lastSyncTime: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: String = "ASC",
    ): Response<PageResponse<InventoryMovement>>

    suspend fun bulkUpsertMovements(movements: List<InventoryMovement>): Result<List<InventoryMovement>>
}
