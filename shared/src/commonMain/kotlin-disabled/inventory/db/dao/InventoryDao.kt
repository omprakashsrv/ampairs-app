package com.ampairs.inventory.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.inventory.db.entity.InventoryEntity

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventoryEntity")
    suspend fun selectAll(): List<InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE id = :id")
    suspend fun selectById(id: String): InventoryEntity?

    @Query("SELECT * FROM inventoryEntity WHERE product_id = :productId")
    suspend fun getInventoryByProduct(productId: String): List<InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE product_id = :productId LIMIT 1")
    suspend fun getInventoryByProductSingle(productId: String): InventoryEntity?

    @Query("SELECT * FROM inventoryEntity WHERE unit_id = :unitId")
    suspend fun getInventoryByUnit(unitId: String): List<InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE stock < :threshold AND active = 1")
    suspend fun getLowStockItems(threshold: Double = 10.0): List<InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE stock = 0 AND active = 1")
    suspend fun getOutOfStockItems(): List<InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE stock > 0 AND active = 1")
    suspend fun getInStockItems(): List<InventoryEntity>

    @Query("UPDATE inventoryEntity SET stock = :newStock, last_updated = :lastUpdated, synced = 0 WHERE id = :id")
    suspend fun updateStock(id: String, newStock: Double, lastUpdated: Long)

    @Query("UPDATE inventoryEntity SET stock = stock + :quantity, last_updated = :lastUpdated, synced = 0 WHERE id = :id")
    suspend fun addStock(id: String, quantity: Double, lastUpdated: Long)

    @Query("UPDATE inventoryEntity SET stock = stock - :quantity, last_updated = :lastUpdated, synced = 0 WHERE id = :id AND stock >= :quantity")
    suspend fun reduceStock(id: String, quantity: Double, lastUpdated: Long): Int

    @Query("UPDATE inventoryEntity SET selling_price = :sellingPrice, last_updated = :lastUpdated, synced = 0 WHERE id = :id")
    suspend fun updateSellingPrice(id: String, sellingPrice: Double, lastUpdated: Long)

    @Query("UPDATE inventoryEntity SET buying_price = :buyingPrice, last_updated = :lastUpdated, synced = 0 WHERE id = :id")
    suspend fun updateBuyingPrice(id: String, buyingPrice: Double, lastUpdated: Long)

    @Query("UPDATE inventoryEntity SET mrp = :mrp, last_updated = :lastUpdated, synced = 0 WHERE id = :id")
    suspend fun updateMrp(id: String, mrp: Double, lastUpdated: Long)

    @Query("SELECT * FROM inventoryEntity WHERE synced = 0")
    suspend fun getUnSyncedInventory(): List<InventoryEntity>

    @Query("SELECT max(last_updated) FROM inventoryEntity")
    suspend fun getLastUpdated(): Long?

    @Query("SELECT count(*) FROM inventoryEntity")
    suspend fun countInventory(): Int

    @Query("SELECT count(*) FROM inventoryEntity WHERE description LIKE ('%' || :searchText || '%')")
    suspend fun countInventoryByDescription(searchText: String): Int

    @Query("SELECT * FROM inventoryEntity WHERE description LIKE ('%' || :searchText || '%') ORDER BY description ASC LIMIT :limit OFFSET :offset")
    suspend fun getInventoryByDescription(searchText: String, limit: Long, offset: Long): List<InventoryEntity>

    @Query("SELECT * FROM inventoryEntity ORDER BY description ASC LIMIT :limit OFFSET :offset")
    suspend fun getInventoryPaged(limit: Long, offset: Long): List<InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE active = 1 ORDER BY description ASC")
    suspend fun getActiveInventory(): List<InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE soft_deleted = 0 ORDER BY description ASC")
    suspend fun getNonDeletedInventory(): List<InventoryEntity>

    @Query("SELECT SUM(stock * buying_price) FROM inventoryEntity WHERE active = 1")
    suspend fun getTotalInventoryValue(): Double?

    @Query("SELECT SUM(stock * selling_price) FROM inventoryEntity WHERE active = 1")
    suspend fun getTotalInventorySellingValue(): Double?

    @Query("SELECT * FROM inventoryEntity WHERE product_id IN (:productIds)")
    suspend fun getInventoryByProductIds(productIds: List<String>): List<InventoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inventory: InventoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inventoryList: List<InventoryEntity>)

    @Update
    suspend fun update(inventory: InventoryEntity)

    @Query("DELETE FROM inventoryEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE inventoryEntity SET soft_deleted = 1, last_updated = :lastUpdated, synced = 0 WHERE id = :id")
    suspend fun softDeleteById(id: String, lastUpdated: Long)

    @Query("DELETE FROM inventoryEntity")
    suspend fun deleteAll()

    // For Paging3 support
    @Query("SELECT * FROM inventoryEntity ORDER BY description ASC")
    fun getInventoryPagingSource(): PagingSource<Int, InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE description LIKE ('%' || :searchText || '%') ORDER BY description ASC")
    fun getInventoryPagingSourceByDescription(searchText: String): PagingSource<Int, InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE product_id = :productId ORDER BY description ASC")
    fun getInventoryPagingSourceByProduct(productId: String): PagingSource<Int, InventoryEntity>

    @Query("SELECT * FROM inventoryEntity WHERE stock < :threshold AND active = 1 ORDER BY stock ASC")
    fun getLowStockItemsPagingSource(threshold: Double = 10.0): PagingSource<Int, InventoryEntity>

    @Transaction
    suspend fun updateInventoryList(inventoryList: List<InventoryEntity>) {
        insertAll(inventoryList)
    }

    @Transaction
    suspend fun performStockTransaction(id: String, quantity: Double, isInward: Boolean, lastUpdated: Long): Boolean {
        return if (isInward) {
            addStock(id, quantity, lastUpdated)
            true
        } else {
            val rowsAffected = reduceStock(id, quantity, lastUpdated)
            rowsAffected > 0
        }
    }
}