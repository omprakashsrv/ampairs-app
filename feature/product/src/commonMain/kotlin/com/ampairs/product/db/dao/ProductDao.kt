package com.ampairs.product.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM productEntity WHERE active = 1 ORDER BY name ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM productEntity WHERE name LIKE '%' || :query || '%' AND active = 1 ORDER BY name ASC")
    fun observeProductsByName(query: String): Flow<List<ProductEntity>>

    // Fast-entry composer lookup (spec 010 v2): name/code substring or tax-code (HSN) prefix.
    @Query(
        """
        SELECT * FROM productEntity
        WHERE active = 1
          AND (name LIKE '%' || :term || '%'
               OR code LIKE '%' || :term || '%'
               OR tax_code LIKE :term || '%')
        ORDER BY name ASC LIMIT :limit
        """
    )
    suspend fun searchForEntry(term: String, limit: Long): List<ProductEntity>

    @Query("SELECT * FROM productEntity WHERE active = 1 ORDER BY name ASC LIMIT :limit")
    suspend fun headProducts(limit: Long): List<ProductEntity>

    @Query("SELECT * FROM productEntity WHERE id = :id")
    fun observeProductById(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM productEntity WHERE id = :id")
    suspend fun productById(id: String): ProductEntity?

    @Query("SELECT * FROM productEntity WHERE name LIKE '%' || :searchText || '%' AND active = 1 ORDER BY name ASC")
    suspend fun getProductsByName(searchText: String): List<ProductEntity>

    @Query("SELECT * FROM productEntity WHERE name LIKE '%' || :searchText || '%' AND group_id IN (:groups) AND active = 1 ORDER BY name ASC")
    suspend fun getProductsByNameAndGroup(searchText: String, groups: List<String>): List<ProductEntity>

    @Query("SELECT max(last_updated) FROM productEntity")
    suspend fun getMaxLastUpdated(): Long?

    @Query("SELECT * FROM productEntity")
    suspend fun getProducts(): List<ProductEntity>

    @Query("SELECT * FROM productEntity WHERE category_id IN (:categoryIds) AND active = 1")
    suspend fun productsByCategoryIds(categoryIds: List<String>): List<ProductEntity>

    @Query("SELECT DISTINCT category_id FROM productEntity WHERE group_id = :groupId AND active = 1")
    suspend fun productCategories(groupId: String): List<String>

    @Query("SELECT count(*) FROM productEntity WHERE group_id = :groupId AND category_id = :categoryId")
    suspend fun countProductsByGroupAndCategory(groupId: String, categoryId: String): Int

    @Query("SELECT * FROM productEntity WHERE group_id = :groupId AND category_id = :categoryId LIMIT :limit OFFSET :offset")
    suspend fun productsByGroupAndCategory(groupId: String, categoryId: String, limit: Long, offset: Long): List<ProductEntity>

    @Query("SELECT * FROM productEntity WHERE id IN (:ids)")
    suspend fun productsByIds(ids: List<String>): List<ProductEntity>

    @Query("SELECT count(*) FROM productEntity WHERE name LIKE ('%' || :name || '%')")
    suspend fun countProductsByName(name: String): Int

    @Query("SELECT * FROM productEntity WHERE name LIKE ('%' || :name || '%') ORDER BY name LIMIT :limit OFFSET :offset")
    suspend fun productsByName(name: String, limit: Long, offset: Long): List<ProductEntity>

    @Query("SELECT count(*) FROM productEntity")
    suspend fun countProducts(): Int

    @Query("SELECT * FROM productEntity ORDER BY name LIMIT :limit OFFSET :offset")
    suspend fun products(limit: Long, offset: Long): List<ProductEntity>

    @Query("SELECT * FROM productEntity WHERE synced = 0")
    suspend fun unSyncedProducts(): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM productEntity WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("SELECT * FROM productEntity WHERE ref_id IN (:refs)")
    suspend fun getProductsByTallyRefIds(refs: List<String>): List<ProductEntity>

    @Query("UPDATE productEntity SET stock_quantity = :quantity WHERE id = :id")
    suspend fun updateStockQuantity(id: String, quantity: Double)

    @Query("UPDATE productEntity SET stock_quantity = :quantity WHERE ref_id = :tallyRefId")
    suspend fun updateStockQuantityByTallyRef(tallyRefId: String, quantity: Double)

    @Query("DELETE FROM productEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM productEntity")
    suspend fun deleteAll()

    // For Paging3 support - these would need to be properly integrated with Room's PagingSource
    @Query("SELECT * FROM productEntity WHERE name LIKE ('%' || :searchText || '%') ORDER BY name")
    fun getProductPagingSource(searchText: String): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM productEntity ORDER BY name")
    fun getAllProductsPagingSource(): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM productEntity WHERE group_id = :groupId AND category_id = :categoryId ORDER BY name")
    fun getProductPagingSourceByGroupAndCategory(groupId: String, categoryId: String): PagingSource<Int, ProductEntity>

    @Transaction
    suspend fun updateProducts(products: List<ProductEntity>) {
        insertAll(products)
    }
}