package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.ProductImageEntity
import com.ampairs.product.db.model.ProductImageModel

@Dao
interface ProductImageDao {

    @Query("SELECT * FROM productImageEntity WHERE id = :id")
    suspend fun productImageById(id: String): ProductImageEntity?

    @Query("SELECT * FROM productImageEntity")
    suspend fun getAllProductImages(): List<ProductImageEntity>

    @Query("SELECT * FROM productImageEntity WHERE product_id = :productId")
    suspend fun getProductImagesByProductId(productId: String): List<ProductImageEntity>

    @Query("SELECT * FROM productImageEntity WHERE image_id = :imageId")
    suspend fun getProductImagesByImageId(imageId: String): List<ProductImageEntity>

    @Transaction
    @Query("SELECT * FROM productImageEntity WHERE product_id IN (:productIds)")
    suspend fun getProductImagesByProductIds(productIds: List<String>): List<ProductImageModel>

    @Query("SELECT * FROM productImageEntity WHERE active = 1")
    suspend fun getActiveProductImages(): List<ProductImageEntity>

    @Query("SELECT * FROM productImageEntity WHERE synced = 0")
    suspend fun unSyncedProductImages(): List<ProductImageEntity>

    // Complex query matching the SQLDelight productImagesByIds query
    @Query("""
        SELECT productImageEntity.*, imageEntity.name, imageEntity.bucket, imageEntity.object_key 
        FROM productImageEntity 
        LEFT JOIN imageEntity ON imageEntity.id = productImageEntity.image_id 
        WHERE productImageEntity.product_id IN (:productIds)
    """)
    suspend fun getProductImagesWithImageDetails(productIds: List<String>): List<ProductImageWithImageDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(productImage: ProductImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productImages: List<ProductImageEntity>)

    @Update
    suspend fun update(productImage: ProductImageEntity)

    @Query("DELETE FROM productImageEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM productImageEntity WHERE product_id = :productId")
    suspend fun deleteByProductId(productId: String)

    @Query("DELETE FROM productImageEntity WHERE image_id = :imageId")
    suspend fun deleteByImageId(imageId: String)

    @Query("DELETE FROM productImageEntity")
    suspend fun deleteAll()

    @Query("UPDATE productImageEntity SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE productImageEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE productImageEntity SET active = :active WHERE id = :id")
    suspend fun updateActiveStatus(id: String, active: Int)

    @Transaction
    suspend fun insertProductImages(productImages: List<ProductImageEntity>) {
        insertAll(productImages)
    }
}

// Data class for the complex query result
data class ProductImageWithImageDetails(
    val id: String,
    val product_id: String,
    val image_id: String,
    val active: Int,
    val soft_deleted: Int,
    val synced: Int,
    val seq_id: Long,
    val name: String?,
    val bucket: String?,
    val object_key: String?
)