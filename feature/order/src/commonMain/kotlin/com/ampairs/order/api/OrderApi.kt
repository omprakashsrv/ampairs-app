package com.ampairs.order.api

import com.ampairs.common.model.Response
import com.ampairs.order.api.model.OrderApiModel

interface OrderApi {

    suspend fun updateOrder(order: OrderApiModel): Response<OrderApiModel>
    suspend fun createInvoice(order: OrderApiModel): Response<OrderApiModel>
    suspend fun getOrders(lastUpdated: Long): Response<List<OrderApiModel>>
}