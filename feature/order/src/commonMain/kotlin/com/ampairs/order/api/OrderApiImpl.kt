package com.ampairs.order.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
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
}
