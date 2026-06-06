package com.ampairs.order.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.order.api.model.OrderApiModel
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine

@Inject @ContributesBinding(AppScope::class)
class OrderApiImpl(engine: HttpClientEngine, tokenRepository: TokenRepository) : OrderApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun updateOrder(order: OrderApiModel): Response<OrderApiModel> {
        return post(
            client,
            ApiUrlBuilder.orderUrl("v1/orders"),
            order
        )
    }

    override suspend fun createInvoice(order: OrderApiModel): Response<OrderApiModel> {
        return post(
            client,
            ApiUrlBuilder.orderUrl("v1/orders/create-invoice"),
            order
        )
    }

    override suspend fun getOrders(lastUpdated: Long): Response<List<OrderApiModel>> {
        return get(
            client,
            ApiUrlBuilder.orderUrl("v1/orders"),
            buildMap {
                put("last_updated", lastUpdated)
            }
        )
    }

    override suspend fun bulkUpdateOrders(orders: List<OrderApiModel>): List<OrderApiModel> {
        val response: Response<List<OrderApiModel>> = post(
            client,
            ApiUrlBuilder.orderUrl("v1/orders/sync"),
            orders
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Bulk order push failed")
        return response.data ?: emptyList()
    }

    override suspend fun getOrdersSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<OrderApiModel> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) params["last_sync"] = lastSync

        val response: Response<PageResponse<OrderApiModel>> = get(
            client,
            ApiUrlBuilder.orderUrl("v1/orders/sync"),
            params
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Order pull failed")
        return response.data ?: PageResponse(
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
}
