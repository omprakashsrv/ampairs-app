package com.ampairs.product.db.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.ampairs.product.db.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM productEntity WHERE active = 1 ORDER BY name ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM productEntity WHERE name LIKE '%' || :query || '%' AND active = 1 ORDER BY name ASC")
    fun observeProductsByName(query: String): Flow<List<ProductEntity>>

    /**
     * Search + multi-select filter (brand / category / sub-category / group) in a single query. Each
     * filter dimension is bypassed when its `hasX` flag is 0. Backed by the `product_brand_idx` /
     * `product_category_idx` / `product_sub_category_idx` / `product_group_idx` indexes.
     */
    @Query(
        """
        SELECT * FROM productEntity
        WHERE active = 1
        AND (:hasBrands = 0 OR brand_id IN (:brands))
        AND (:hasCategories = 0 OR category_id IN (:categories))
        AND (:hasSubCategories = 0 OR sub_category_id IN (:subCategories))
        AND (:hasGroups = 0 OR group_id IN (:groups))
        AND (:query = '' OR name LIKE '%' || :query || '%')
        ORDER BY name ASC
        """
    )
    fun filterProducts(
        query: String,
        brands: List<String>,
        hasBrands: Int,
        categories: List<String>,
        hasCategories: Int,
        subCategories: List<String>,
        hasSubCategories: Int,
        groups: List<String>,
        hasGroups: Int,
    ): Flow<List<ProductEntity>>

    @Query(
        "SELECT DISTINCT brand_id FROM productEntity WHERE active = 1 AND brand_id IS NOT NULL AND brand_id != '' ORDER BY brand_id ASC"
    )
    suspend fun getDistinctBrandIds(): List<String>

    @Query(
        "SELECT DISTINCT category_id FROM productEntity WHERE active = 1 AND category_id IS NOT NULL AND category_id != '' ORDER BY category_id ASC"
    )
    suspend fun getDistinctCategoryIds(): List<String>

    @Query(
        "SELECT DISTINCT sub_category_id FROM productEntity WHERE active = 1 AND sub_category_id IS NOT NULL AND sub_category_id != '' ORDER BY sub_category_id ASC"
    )
    suspend fun getDistinctSubCategoryIds(): List<String>

    @Query(
        "SELECT DISTINCT group_id FROM productEntity WHERE active = 1 AND group_id IS NOT NULL AND group_id != '' ORDER BY group_id ASC"
    )
    suspend fun getDistinctGroupIds(): List<String>

    // Fast-entry composer lookup (spec 010 v2): name/code/tax-code partial word search.
    @Query(
        """
        SELECT * FROM productEntity
        WHERE active = 1
          AND (name LIKE '%' || :term || '%'
               OR code LIKE '%' || :term || '%'
               OR tax_code LIKE '%' || :term || '%')
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

    @Query("UPDATE productEntity SET stock_quantity = :quantity, synced = 0 WHERE id = :id")
    suspend fun updateStockQuantity(id: String, quantity: Double)

    @Query("UPDATE productEntity SET stock_quantity = :quantity, synced = 0 WHERE ref_id = :tallyRefId")
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