package com.ampairs.inventory.db

import androidx.paging.PagingSource
import com.ampairs.inventory.api.InventoryApi
import com.ampairs.inventory.db.dao.InventoryDao
import com.ampairs.inventory.db.entity.InventoryEntity
import com.ampairs.inventory.domain.Inventory
import com.ampairs.inventory.domain.asDatabaseModel
import com.ampairs.inventory.domain.asDomainModel
import com.ampairs.inventory.domain.asInventoryApiModel
import com.ampairs.common.model.Response
import com.ampairs.product.api.model.InventoryApiModel

class InventoryRepository(
    val inventoryDao: InventoryDao,
    val inventoryApi: InventoryApi
) {

    fun getInventoryPaging(searchText: String): PagingSource<Int, InventoryEntity> {
        return inventoryDao.getInventoryPagingSourceByDescription(searchText)
    }

    suspend fun getInventory(inventoryId: String): InventoryEntity? {
        return inventoryDao.selectById(inventoryId)
    }

    suspend fun getProductInventory(productId: String): Inventory? {
        return inventoryDao.getInventoryByProductSingle(productId)?.asDomainModel()
    }

    suspend fun getInventories(productIds: List<String>): List<Inventory> {
        return inventoryDao.getInventoryByProductIds(productIds).asDomainModel()
    }

    suspend fun updateInventory(inventory: InventoryEntity) {
        inventoryDao.insert(inventory)
        val inventories = inventoryDao.getUnSyncedInventory()
        val updatedProducts = updateInventories(inventories.asInventoryApiModel())
        updatedProducts.data?.map {
            it.lastUpdated = 0
            it
        }?.asDatabaseModel()?.let {
            inventoryDao.updateInventoryList(it)
        }
    }

    suspend fun updateInventories(products: List<InventoryApiModel>): Response<List<InventoryApiModel>> {
        return inventoryApi.updateProducts(products)
    }
}