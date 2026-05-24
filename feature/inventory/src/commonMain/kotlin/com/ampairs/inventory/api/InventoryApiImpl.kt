package com.ampairs.inventory.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.model.Response
import com.ampairs.product.api.model.InventoryApiModel
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine

@Inject @ContributesBinding(AppScope::class)
class InventoryApiImpl(engine: HttpClientEngine, tokenRepository: TokenRepository) : InventoryApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getProducts(
        lastUpdated: Long?,
        groupId: String?,
    ): Response<List<InventoryApiModel>> {
        return get(
            client,
            ApiUrlBuilder.inventoryUrl("v1/items"),
            buildMap {
                lastUpdated?.let { put("last_updated", it) }
                groupId?.let { put("group_id", it) }
            })
    }

    override suspend fun updateProducts(products: List<InventoryApiModel>): Response<List<InventoryApiModel>> {
        return post(
            client,
            ApiUrlBuilder.inventoryUrl("v1/items"),
            products
        )
    }
}
