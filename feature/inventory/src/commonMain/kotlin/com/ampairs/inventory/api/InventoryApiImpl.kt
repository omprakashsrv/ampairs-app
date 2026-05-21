package com.ampairs.inventory.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.model.Response
import com.ampairs.product.api.model.InventoryApiModel
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine


const val PRODUCT_ENDPOINT = "http://localhost:8080"

@Inject @ContributesBinding(AppScope::class)
class InventoryApiImpl(engine: HttpClientEngine, tokenRepository: TokenRepository) : InventoryApi {

    private val client = httpClient(engine, tokenRepository)
    override suspend fun getProducts(
        lastUpdated: Long?,
        groupId: String?,
    ): Response<List<InventoryApiModel>> {
        return get(
            client,
            PRODUCT_ENDPOINT + "/inventpry/v1",
            buildMap {
                lastUpdated?.let { put("last_updated", it) }
                groupId?.let { put("group_id", it) }
            })
    }

    override suspend fun updateProducts(products: List<InventoryApiModel>): Response<List<InventoryApiModel>> {
        return post(
            client,
            "${PRODUCT_ENDPOINT}/inventory/v1/inventories",
            products
        )
    }
}