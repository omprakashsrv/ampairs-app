package com.ampairs.product.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.ampairs.product.db.entity.ProductVariantEntity
import kotlinx.coroutines.flow.Flow

/**
 * Product Variant DAO - Database operations for product variants
 */
@Dao
interface ProductVariantDao {

    /**
     * Observe all variants for a product (reactive)
     */
    @Query("""
        SELECT * FROM product_variants
        WHERE product_id = :productId
        AND active = 1
        ORDER BY variant_name ASC
    """)
    fun observeProductVariants(productId: String): Flow<List<ProductVariantEntity>>

    /**
     * Get variant by ID
     */
    @Query("""
        SELECT * FROM product_variants
        WHERE id = :variantId
        LIMIT 1
    """)
    suspend fun getVariantById(variantId: String): ProductVariantEntity?

    /**
     * Get variant by SKU
     */
    @Query("""
        SELECT * FROM product_variants
        WHERE sku = :sku
        LIMIT 1
    """)
    suspend fun getVariantBySku(sku: String): ProductVariantEntity?

    /**
     * Get all variants for a product (non-reactive)
     */
    @Query("""
        SELECT * FROM product_variants
        WHERE product_id = :productId
        AND active = 1
        ORDER BY variant_name ASC
    """)
    suspend fun getProductVariants(productId: String): List<ProductVariantEntity>

    /**
     * Get total stock across all variants for a product
     */
    @Query("""
        SELECT COALESCE(SUM(stock_quantity), 0.0) FROM product_variants
        WHERE product_id = :productId
        AND active = 1
    """)
    suspend fun getTotalProductStock(productId: String): Double

    /**
     * Insert or update variant
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariantEntity)

    /**
     * Insert or update multiple variants
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariants(variants: List<ProductVariantEntity>)

    /**
     * Update variant
     */
    @Update
    suspend fun updateVariant(variant: ProductVariantEntity)

    /**
     * Soft delete variant (mark as inactive)
     */
    @Query("""
        UPDATE product_variants
        SET active = 0
        WHERE id = :variantId
    """)
    suspend fun deleteVariant(variantId: String)

    /**
     * Hard delete variant (permanent)
     */
    @Query("""
        DELETE FROM product_variants
        WHERE id = :variantId
    """)
    suspend fun permanentlyDeleteVariant(variantId: String)

    /**
     * Get unsynced variants for background sync
     */
    @Query("""
        SELECT * FROM product_variants
        WHERE synced = 0
        ORDER BY updated_at ASC
    """)
    suspend fun getUnsyncedVariants(): List<ProductVariantEntity>

    /**
     * Update sync status
     */
    @Query("""
        UPDATE product_variants
        SET synced = :synced
        WHERE id = :variantId
    """)
    suspend fun updateSyncStatus(variantId: String, synced: Boolean)
}
