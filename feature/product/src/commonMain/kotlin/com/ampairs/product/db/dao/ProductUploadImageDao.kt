package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ampairs.product.db.entity.ProductUploadImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductUploadImageDao {

    @Query("SELECT * FROM product_upload_images WHERE product_id = :productId ORDER BY sort_order ASC, local_created_at DESC")
    fun observeProductImages(productId: String): Flow<List<ProductUploadImageEntity>>

    @Query("SELECT * FROM product_upload_images WHERE product_id = :productId ORDER BY sort_order ASC, local_created_at DESC")
    suspend fun getProductImages(productId: String): List<ProductUploadImageEntity>

    @Query("SELECT * FROM product_upload_images WHERE uid = :uid")
    suspend fun getProductImage(uid: String): ProductUploadImageEntity?

    @Query("SELECT DISTINCT product_id FROM product_upload_images")
    suspend fun getDistinctProductIds(): List<String>

    @Query("SELECT * FROM product_upload_images WHERE synced = 0 ORDER BY local_created_at ASC")
    suspend fun getUnsyncedImages(): List<ProductUploadImageEntity>

    @Query("SELECT COUNT(*) FROM product_upload_images WHERE product_id = :productId")
    suspend fun getImageCount(productId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductImage(entity: ProductUploadImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductImages(entities: List<ProductUploadImageEntity>)

    @Query("DELETE FROM product_upload_images WHERE uid = :uid")
    suspend fun deleteProductImage(uid: String)

    @Query("DELETE FROM product_upload_images WHERE product_id = :productId")
    suspend fun deleteProductImages(productId: String)

    @Query("UPDATE product_upload_images SET upload_status = :status, local_updated_at = :updatedAt WHERE uid = :uid")
    suspend fun updateUploadStatus(uid: String, status: String, updatedAt: String)

    @Query("UPDATE product_upload_images SET is_primary = 0, local_updated_at = :updatedAt WHERE product_id = :productId")
    suspend fun clearPrimaryImages(productId: String, updatedAt: String)

    @Query("UPDATE product_upload_images SET is_primary = 1, local_updated_at = :updatedAt WHERE uid = :uid")
    suspend fun setPrimaryImage(uid: String, updatedAt: String)

    @Transaction
    suspend fun setPrimaryImageAtomic(productId: String, uid: String, updatedAt: String) {
        clearPrimaryImages(productId, updatedAt)
        setPrimaryImage(uid, updatedAt)
    }

    @Query("SELECT COUNT(*) FROM product_upload_images WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int
}
