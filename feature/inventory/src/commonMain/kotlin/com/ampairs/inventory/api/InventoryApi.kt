package com.ampairs.inventory.api

import com.ampairs.common.model.Response
import com.ampairs.product.api.model.InventoryApiModel

interface InventoryApi {

    suspend fun getProducts(lastUpdated: Long?, groupId: String?): Response<List<InventoryApiModel>>
    suspend fun updateProducts(products: List<InventoryApiModel>): Response<List<InventoryApiModel>>

}