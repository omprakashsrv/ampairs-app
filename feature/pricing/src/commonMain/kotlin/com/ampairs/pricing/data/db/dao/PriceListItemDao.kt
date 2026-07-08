package com.ampairs.pricing.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.pricing.data.db.entity.PriceListItemEntity

@Dao
interface PriceListItemDao {

    @Query("SELECT * FROM price_list_items WHERE id = :id")
    suspend fun getItemById(id: String): PriceListItemEntity?

    @Query("SELECT * FROM price_list_items WHERE price_list_id = :priceListId AND active = 1")
    suspend fun getItemsForPriceList(priceListId: String): List<PriceListItemEntity>

    @Query("SELECT * FROM price_list_items WHERE product_id = :productId AND active = 1")
    suspend fun getItemsForProduct(productId: String): List<PriceListItemEntity>

    @Query("SELECT * FROM price_list_items WHERE synced = 0")
    suspend fun getUnsyncedItems(): List<PriceListItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PriceListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PriceListItemEntity>)

    @Query("DELETE FROM price_list_items WHERE price_list_id = :priceListId")
    suspend fun deleteItemsForPriceList(priceListId: String)

    @Query("DELETE FROM price_list_items WHERE id = :id")
    suspend fun hardDeleteItem(id: String)

    @Query("DELETE FROM price_list_items")
    suspend fun clearAll()
}
