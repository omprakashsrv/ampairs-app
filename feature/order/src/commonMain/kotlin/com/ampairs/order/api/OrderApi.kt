package com.ampairs.order.api

import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.order.api.model.OrderApiModel

interface OrderApi {

    suspend fun createInvoice(order: OrderApiModel): Response<OrderApiModel>
    suspend fun getOrders(lastUpdated: Long): Response<List<OrderApiModel>>

    /** Bulk upsert for offline sync (spec 010). Server stores taxInfos/totals as supplied. */
    suspend fun bulkUpdateOrders(orders: List<OrderApiModel>): List<OrderApiModel>

    /** Paginated incremental pull for offline sync (spec 010). */
    suspend fun getOrdersSync(
        lastSync: String = "",
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<OrderApiModel>
}