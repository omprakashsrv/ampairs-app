package com.ampairs.purchase.api

import com.ampairs.common.model.PageResponse
import com.ampairs.purchase.api.model.PurchaseApiModel

interface PurchaseApi {

    /** Bulk upsert for offline sync. Server stores taxInfos/totals as supplied. */
    suspend fun bulkUpdatePurchases(purchases: List<PurchaseApiModel>): List<PurchaseApiModel>

    /** Paginated incremental pull for offline sync (`GET v1/purchases/sync`). */
    suspend fun getPurchases(
        lastSync: String = "",
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<PurchaseApiModel>
}
