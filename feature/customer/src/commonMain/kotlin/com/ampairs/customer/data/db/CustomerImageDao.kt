package com.ampairs.customer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for customer image database operations.
 * Supports offline-first patterns with sync metadata tracking.
 */
@Dao
interface CustomerImageDao {

    // Basic CRUD operations

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerImage(customerImage: CustomerImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerImages(customerImages: List<CustomerImageEntity>)

    @Update
    suspend fun updateCustomerImage(customerImage: CustomerImageEntity)

    @Query("DELETE FROM customer_images WHERE uid = :uid")
    suspend fun deleteCustomerImage(uid: String)

    @Query("DELETE FROM customer_images WHERE customer_id = :customerId")
    suspend fun deleteCustomerImagesByCustomer(customerId: String)

    // Query operations (workspace isolation handled at database file level)

    @Query("SELECT * FROM customer_images WHERE uid = :uid")
    suspend fun getCustomerImage(uid: String): CustomerImageEntity?

    @Query("SELECT * FROM customer_images WHERE customer_id = :customerId ORDER BY sort_order ASC, created_at DESC")
    fun observeCustomerImages(customerId: String): Flow<List<CustomerImageEntity>>

    @Query("SELECT * FROM customer_images WHERE customer_id = :customerId ORDER BY sort_order ASC, created_at DESC")
    suspend fun getCustomerImages(customerId: String): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE customer_id = :customerId AND is_primary = 1 LIMIT 1")
    suspend fun getPrimaryCustomerImage(customerId: String): CustomerImageEntity?

    @Query("SELECT * FROM customer_images WHERE customer_id = :customerId AND is_primary = 1 LIMIT 1")
    fun observePrimaryCustomerImage(customerId: String): Flow<CustomerImageEntity?>

    @Query("SELECT * FROM customer_images WHERE is_primary = 1")
    suspend fun getAllPrimaryImages(): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE is_primary = 1 AND customer_id IN (:customerIds)")
    suspend fun getPrimaryImagesForCustomers(customerIds: List<String>): List<CustomerImageEntity>

    // Sync-related queries

    @Query("SELECT * FROM customer_images WHERE synced = 0 ORDER BY local_created_at ASC")
    suspend fun getUnsyncedCustomerImages(): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE sync_pending = 1 ORDER BY local_updated_at ASC")
    suspend fun getPendingSyncCustomerImages(): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE upload_status = :status")
    suspend fun getCustomerImagesByUploadStatus(status: String): List<CustomerImageEntity>

    @Query("UPDATE customer_images SET synced = :synced, sync_pending = :syncPending, last_sync_attempt = :lastSyncAttempt WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, synced: Boolean, syncPending: Boolean, lastSyncAttempt: String?)

    @Query("UPDATE customer_images SET upload_status = :status, local_updated_at = :updatedAt WHERE uid = :uid")
    suspend fun updateUploadStatus(uid: String, status: String, updatedAt: String)

    // Primary image management

    @Query("UPDATE customer_images SET is_primary = 0, local_updated_at = :updatedAt WHERE customer_id = :customerId")
    suspend fun clearPrimaryImages(customerId: String, updatedAt: String)

    @Query("UPDATE customer_images SET is_primary = 1, local_updated_at = :updatedAt WHERE uid = :uid")
    suspend fun setPrimaryImage(uid: String, updatedAt: String)

    @Query("""
        UPDATE customer_images
        SET is_primary = 1,
            synced = :synced,
            sync_pending = :syncPending,
            last_sync_attempt = :lastSyncAttempt,
            local_updated_at = :updatedAt
        WHERE uid = :uid
    """)
    suspend fun setPrimaryImageWithSyncStatus(
        uid: String,
        synced: Boolean,
        syncPending: Boolean,
        lastSyncAttempt: String?,
        updatedAt: String
    )

    /**
     * Atomically set an image as primary and update sync status.
     * Combines clearPrimaryImages + setPrimaryImageWithSyncStatus in a single transaction.
     */
    @Transaction
    suspend fun setPrimaryImageAtomic(
        customerId: String,
        imageId: String,
        updatedAt: String
    ) {
        clearPrimaryImages(customerId, updatedAt)
        setPrimaryImageWithSyncStatus(
            uid = imageId,
            synced = false,
            syncPending = true,
            lastSyncAttempt = null,
            updatedAt = updatedAt
        )
    }

    // Batch operations

    @Query("DELETE FROM customer_images WHERE uid IN (:uids)")
    suspend fun deleteCustomerImagesByIds(uids: List<String>)

    @Query("UPDATE customer_images SET sort_order = :sortOrder, local_updated_at = :updatedAt WHERE uid = :uid")
    suspend fun updateSortOrder(uid: String, sortOrder: Int, updatedAt: String)

    // File management

    @Query("SELECT local_path FROM customer_images WHERE local_path IS NOT NULL")
    suspend fun getAllLocalPaths(): List<String>

    @Query("UPDATE customer_images SET local_path = :localPath, local_updated_at = :updatedAt WHERE uid = :uid")
    suspend fun updateLocalPath(uid: String, localPath: String?, updatedAt: String)

    // Statistics and counts

    @Query("SELECT COUNT(*) FROM customer_images WHERE customer_id = :customerId")
    suspend fun getCustomerImageCount(customerId: String): Int

    @Query("SELECT COUNT(*) FROM customer_images WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT COUNT(*) FROM customer_images WHERE upload_status = 'PENDING'")
    suspend fun getPendingUploadCount(): Int

    // Search and filtering

    @Query("""
        SELECT * FROM customer_images
        WHERE customer_id = :customerId
        AND (file_name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY sort_order ASC, created_at DESC
    """)
    suspend fun searchCustomerImages(customerId: String, query: String): List<CustomerImageEntity>

    @Query("SELECT * FROM customer_images WHERE updated_at > :lastSyncTime ORDER BY updated_at ASC")
    suspend fun getModifiedSince(lastSyncTime: String): List<CustomerImageEntity>

    // Cleanup operations

    @Query("DELETE FROM customer_images")
    suspend fun deleteAll()

    @Query("DELETE FROM customer_images WHERE local_path IS NOT NULL AND synced = 1")
    suspend fun deleteLocalFilesForSyncedImages()
}