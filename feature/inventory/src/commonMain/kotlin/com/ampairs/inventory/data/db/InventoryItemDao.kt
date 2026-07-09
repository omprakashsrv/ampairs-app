package com.ampairs.inventory.data.db

import androidx.room3.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryItemDao {

    @Query("SELECT * FROM inventory_items WHERE active = 1 ORDER BY name ASC")
    fun getAllItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE active = 1 AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchItems(query: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE active = 1 AND reorderLevel > 0 AND currentStock <= reorderLevel AND currentStock > 0 ORDER BY currentStock ASC")
    fun getLowStockItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE active = 1 AND currentStock <= 0 ORDER BY name ASC")
    fun getOutOfStockItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE active = 0 ORDER BY name ASC")
    fun getInactiveItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllItemsIncludingInactive(): Flow<List<InventoryItemEntity>>

    @Query("SELECT COUNT(*) FROM inventory_items WHERE active = 1")
    fun getActiveItemCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(currentStock * costPrice), 0) FROM inventory_items WHERE active = 1")
    fun getTotalStockValue(): Flow<Double>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    fun observeItemById(id: String): Flow<InventoryItemEntity?>

    @Query("SELECT * FROM inventory_items WHERE productId = :productId AND active = 1 LIMIT 1")
    suspend fun getItemByProduct(productId: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE productId = :productId AND active = 1 LIMIT 1")
    fun observeItemByProduct(productId: String): Flow<InventoryItemEntity?>

    @Query("SELECT * FROM inventory_items WHERE synced = 0")
    suspend fun getUnsyncedItems(): List<InventoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItemEntity>)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun hardDeleteItem(id: String)

    @Query("DELETE FROM inventory_items")
    suspend fun clearAll()
}
