package com.ampairs.customer.data.repository

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.api.CustomerImageApi
import com.ampairs.customer.data.db.CustomerImageDao
import com.ampairs.customer.data.db.CustomerImageEntity
import com.ampairs.customer.data.db.toCustomerImage
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.data.db.toListItem
import com.ampairs.customer.domain.CustomerImage
import com.ampairs.customer.domain.CustomerImageListItem
import com.ampairs.customer.domain.CustomerImageUploadRequest
import com.ampairs.customer.domain.CustomerImageUpdateRequest
import com.ampairs.customer.domain.CustomerImageStatus
import com.ampairs.customer.util.CustomerConstants.ERROR_CUSTOMER_IMAGE_UID_REQUIRED
import com.ampairs.customer.util.CustomerLogger
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository for customer image operations with offline-first architecture.
 * Provides database-first operations with background server synchronization.
 */
class CustomerImageRepository(
    private val dao: CustomerImageDao,
    private val api: CustomerImageApi,
    private val appPreferences: AppPreferencesDataStore,
    private val fileManager: PlatformFileManager
) {

    // Observing operations

    fun observeCustomerImages(customerId: String): Flow<List<CustomerImageListItem>> {
        return dao.observeCustomerImages(customerId)
            .map { entities -> entities.map { it.toListItem() } }
    }

    fun observePrimaryImage(customerId: String): Flow<CustomerImage?> {
        return dao.observePrimaryCustomerImage(customerId)
            .map { it?.toCustomerImage() }
    }

    suspend fun getCustomerImages(customerId: String): List<CustomerImageListItem> {
        return dao.getCustomerImages(customerId)
            .map { it.toListItem() }
    }

    suspend fun getCustomerImage(imageId: String): CustomerImage? {
        return dao.getCustomerImage(imageId)?.toCustomerImage()
    }

    suspend fun getPrimaryImage(customerId: String): CustomerImage? {
        return dao.getPrimaryCustomerImage(customerId)?.toCustomerImage()
    }

    // Image upload operations

    @OptIn(ExperimentalTime::class)
    suspend fun uploadImage(
        customerId: String,
        fileName: String,
        contentType: String,
        fileSize: Long,
        imageData: ByteArray,
        description: String? = null,
        isPrimary: Boolean = false
    ): Result<CustomerImage> {
        val uid = UidGenerator.generateUid("IMG")
        val now = Clock.System.now().toString()

        // Create upload request
        val uploadRequest = CustomerImageUploadRequest(
            customerId = customerId,
            fileName = fileName,
            contentType = contentType,
            fileSize = fileSize,
            description = description,
            isPrimary = isPrimary,
            sortOrder = getNextSortOrder(customerId)
        )

        // Create local customer image
        val customerImage = CustomerImage(
            uid = uid,
            customerId = customerId,
            fileName = fileName,
            contentType = contentType,
            fileSize = fileSize,
            description = description,
            isPrimary = isPrimary,
            sortOrder = uploadRequest.sortOrder,
            uploadStatus = CustomerImageStatus.PENDING,
            localPath = null // Will be set after local file save
        )

        // 1. Save image file locally first for offline access
        val localPath = try {
            fileManager.saveImageToCache(uid, imageData, fileName)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Failed to save image locally", e)
            null
        }

        // 2. Create initial entity with all data (single DB write)
        val initialImage = customerImage.copy(
            localPath = localPath ?: "",
            uploadStatus = CustomerImageStatus.UPLOADING
        )
        val initialEntity = initialImage.toEntity(synced = false, localCreatedAt = now, localUpdatedAt = now)
        dao.insertCustomerImage(initialEntity)

        // 3. Handle primary image logic if needed
        if (isPrimary) {
            dao.clearPrimaryImages(customerId, now)
            dao.setPrimaryImage(uid, now)
        }

        // 4. Background server upload using multipart form data
        try {
            // Wrap upload operation in timeout (60 seconds)
            val serverImage = withTimeout(60_000L) {
                // Direct multipart upload to backend
                val uploadResponse = api.uploadCustomerImageMultipart(
                    uid = customerImage.uid,
                    customerId = customerId,
                    fileName = fileName,
                    contentType = contentType,
                    imageData = imageData,
                    description = description,
                    isPrimary = isPrimary,
                    displayOrder = uploadRequest.sortOrder
                )

                // Create final synced entity with server URLs (single DB write)
                val syncedImage = initialImage.copy(
                    imageUrl = uploadResponse.imageUrl,
                    thumbnailUrl = uploadResponse.thumbnailUrl,
                    uploadStatus = CustomerImageStatus.COMPLETED
                )
                val syncedEntity = syncedImage.toEntity(synced = true, localCreatedAt = now, localUpdatedAt = now)
                dao.insertCustomerImage(syncedEntity)

                CustomerLogger.i("CustomerImageRepository", "Multipart upload successful for: $fileName")
                syncedImage
            }
            return Result.success(serverImage)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CustomerLogger.w("CustomerImageRepository", "Upload timeout for: $fileName after 60 seconds")
            dao.updateUploadStatus(uid, CustomerImageStatus.FAILED, now)
            return Result.success(initialImage.copy(uploadStatus = CustomerImageStatus.FAILED))
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Background multipart upload failed", e)
            dao.updateUploadStatus(uid, CustomerImageStatus.FAILED, now)
            return Result.success(initialImage.copy(uploadStatus = CustomerImageStatus.FAILED))
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun updateImage(imageId: String, updateRequest: CustomerImageUpdateRequest): Result<CustomerImage> {
        val existing = dao.getCustomerImage(imageId)
            ?: return Result.failure(Exception("Image not found"))

        val now = Clock.System.now().toString()

        // Create updated image
        val updatedImage = existing.toCustomerImage().copy(
            description = updateRequest.description ?: existing.description,
            isPrimary = updateRequest.isPrimary ?: existing.isPrimary,
            sortOrder = updateRequest.sortOrder ?: existing.sortOrder,
            tags = updateRequest.tags,
            metadata = updateRequest.metadata
        )

        // 1. Update local database first (offline-first)
        val unsyncedEntity = updatedImage.toEntity(synced = false, localUpdatedAt = now)
        dao.insertCustomerImage(unsyncedEntity)

        // 2. Handle primary image logic
        if (updateRequest.isPrimary == true) {
            dao.clearPrimaryImages(existing.customerId, now)
            dao.setPrimaryImage(imageId, now)
        }

        // 3. Background server sync
        try {
            val serverImage = api.updateCustomerImage(existing.customerId, imageId, updateRequest)
            // Merge server response with local fields to preserve localPath, uploadStatus
            val mergedImage = serverImage.copy(
                localPath = existing.localPath,
                uploadStatus = existing.uploadStatus
            )
            val syncedEntity = mergedImage.toEntity(synced = true, localCreatedAt = existing.localCreatedAt, localUpdatedAt = now)
            dao.insertCustomerImage(syncedEntity)
            return Result.success(mergedImage)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Failed to sync image update to server", e)
            return Result.success(updatedImage)
        }
    }

    suspend fun deleteImage(imageId: String): Result<Unit> {
        val existing = dao.getCustomerImage(imageId)
            ?: return Result.failure(Exception("Image not found"))

        // 1. Delete from local database first
        dao.deleteCustomerImage(imageId)

        // 2. Delete local file if exists
        existing.localPath?.let { localPath ->
            try {
                fileManager.deleteFile(localPath)
            } catch (e: Exception) {
                CustomerLogger.w("CustomerImageRepository", "Failed to delete local file: $localPath", e)
            }
        }

        // 3. Background server delete
        try {
            api.deleteCustomerImage(existing.customerId, imageId)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Failed to delete image from server", e)
            // Note: Since image is already deleted locally, we don't restore it
            // Server sync will handle cleanup on next sync cycle
        }

        return Result.success(Unit)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun setPrimaryImage(imageId: String): Result<CustomerImage> {
        val existing = dao.getCustomerImage(imageId)
            ?: return Result.failure(Exception("Image not found"))

        val now = Clock.System.now().toString()

        // If already primary, do nothing (no unset option)
        if (existing.isPrimary) {
            CustomerLogger.i("CustomerImageRepository", "Image is already primary: $imageId")
            return Result.success(existing.toCustomerImage())
        }

        // Set as primary (optimized: single transaction instead of 3 separate queries)
        dao.setPrimaryImageAtomic(existing.customerId, imageId, now)

        val updatedImage = existing.toCustomerImage().copy(isPrimary = true)

        // 3. Background server sync
        try {
            val serverImage = api.setPrimaryImage(existing.customerId, imageId)
            // Merge server response with local fields to preserve localPath, uploadStatus
            val mergedImage = serverImage.copy(
                localPath = existing.localPath,
                uploadStatus = existing.uploadStatus
            )
            val syncedEntity = mergedImage.toEntity(synced = true, localCreatedAt = existing.localCreatedAt, localUpdatedAt = now)
            dao.insertCustomerImage(syncedEntity)
            return Result.success(mergedImage)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Failed to sync primary image to server", e)
            return Result.success(updatedImage)
        }
    }

    // Batch synchronization

    @OptIn(ExperimentalTime::class)
    suspend fun syncCustomerImages(customerId: String): Result<Int> {
        return try {
            CustomerLogger.i("CustomerImageSync", "Starting customer image sync for customer: $customerId")

            // 0. Clean up stale UPLOADING records (older than 5 minutes)
            cleanupStaleUploadingRecords(customerId)

            // 1. Sync unsynced local images to server first
            val unsyncedImages = dao.getUnsyncedCustomerImages().filter { it.customerId == customerId }
            var syncedCount = 0
            val now = Clock.System.now().toString()

            // Batch collect entities to update after all operations
            val entitiesToUpdate = mutableListOf<CustomerImageEntity>()

            for (entity in unsyncedImages) {
                try {
                    if (entity.uploadStatus == CustomerImageStatus.PENDING || entity.uploadStatus == CustomerImageStatus.FAILED) {
                        // Handle pending and failed uploads - retry upload using multipart
                        if (entity.localPath != null && fileManager.fileExists(entity.localPath)) {
                            try {
                                // Update to UPLOADING status (temporary, will be replaced on success)
                                val uploadingEntity = entity.copy(uploadStatus = CustomerImageStatus.UPLOADING, localUpdatedAt = now)
                                entitiesToUpdate.add(uploadingEntity)

                                // Wrap upload operation in timeout (60 seconds)
                                withTimeout(60_000L) {
                                    // Read the file data from local storage
                                    val imageData = fileManager.readFile(entity.localPath)
                                    CustomerLogger.d("CustomerImageSync", "Read ${imageData.size} bytes from local file for retry: ${entity.uid}")

                                    // Perform multipart upload using the existing API
                                    val uploadResponse = api.uploadCustomerImageMultipart(
                                        uid = entity.uid,
                                        customerId = entity.customerId,
                                        fileName = entity.fileName,
                                        contentType = entity.contentType,
                                        imageData = imageData,
                                        description = entity.description,
                                        isPrimary = entity.isPrimary,
                                        displayOrder = entity.sortOrder
                                    )

                                    // Create synced entity with server URLs
                                    val syncedImage = entity.toCustomerImage().copy(
                                        imageUrl = uploadResponse.imageUrl,
                                        thumbnailUrl = uploadResponse.thumbnailUrl,
                                        uploadStatus = CustomerImageStatus.COMPLETED
                                    )
                                    val syncedEntity = syncedImage.toEntity(synced = true, localCreatedAt = entity.localCreatedAt, localUpdatedAt = now)

                                    // Replace UPLOADING entity with COMPLETED entity
                                    entitiesToUpdate.removeAll { it.uid == entity.uid }
                                    entitiesToUpdate.add(syncedEntity)
                                    syncedCount++

                                    CustomerLogger.i("CustomerImageSync", "Successfully retried upload for: ${entity.uid}")
                                }
                            } catch (e: Exception) {
                                val errorMsg = if (e is kotlinx.coroutines.TimeoutCancellationException) {
                                    "Upload timeout for image ${entity.uid} after 60 seconds"
                                } else {
                                    "Failed to retry upload for image ${entity.uid}: ${e.message}"
                                }
                                CustomerLogger.w("CustomerImageSync", errorMsg)

                                // Replace with FAILED entity
                                val failedEntity = entity.copy(uploadStatus = CustomerImageStatus.FAILED, localUpdatedAt = now)
                                entitiesToUpdate.removeAll { it.uid == entity.uid }
                                entitiesToUpdate.add(failedEntity)
                            }
                        } else {
                            // Local file not found, mark as failed
                            CustomerLogger.w("CustomerImageSync", "Local file not found for upload retry: ${entity.uid}")
                            val failedEntity = entity.copy(uploadStatus = CustomerImageStatus.FAILED, localUpdatedAt = now)
                            entitiesToUpdate.add(failedEntity)
                        }
                    } else {
                        // Handle metadata updates
                        val image = entity.toCustomerImage()
                        val updateRequest = CustomerImageUpdateRequest(
                            description = image.description,
                            isPrimary = image.isPrimary,
                            sortOrder = image.sortOrder,
                            tags = image.tags,
                            metadata = image.metadata
                        )
                        val serverImage = api.updateCustomerImage(image.customerId, image.uid, updateRequest)
                        // Merge server response with local fields to preserve localPath, uploadStatus
                        val mergedImage = serverImage.copy(
                            localPath = entity.localPath,
                            uploadStatus = entity.uploadStatus
                        )
                        val syncedEntity = mergedImage.toEntity(synced = true, localCreatedAt = entity.localCreatedAt, localUpdatedAt = now)
                        entitiesToUpdate.add(syncedEntity)
                        syncedCount++
                    }
                } catch (e: Exception) {
                    CustomerLogger.e("CustomerImageSync", "Failed to sync image ${entity.uid}", e)
                }
            }

            // 2. Fetch server images and prepare batch update
            val lastSyncTime = appPreferences.getCustomerLastSyncTime().first()
            val serverImages = api.getCustomerImages(customerId, lastSyncTime)

            // Fetch all existing entities at once to reduce DB calls
            val existingEntitiesMap = dao.getCustomerImages(customerId).associateBy { it.uid }

            for (serverImage in serverImages) {
                val existing = existingEntitiesMap[serverImage.uid]
                if (existing == null) {
                    // New image from server - insert with COMPLETED status
                    val entity = serverImage.copy(
                        uploadStatus = CustomerImageStatus.COMPLETED,
                        localPath = null
                    ).toEntity(synced = true, localCreatedAt = now, localUpdatedAt = now)
                    entitiesToUpdate.add(entity)
                    syncedCount++
                } else if (existing.synced) {
                    // Existing synced image - preserve local fields (localPath, uploadStatus)
                    val mergedImage = serverImage.copy(
                        uploadStatus = existing.uploadStatus,
                        localPath = existing.localPath
                    )
                    val entity = mergedImage.toEntity(
                        synced = true,
                        localCreatedAt = existing.localCreatedAt,
                        localUpdatedAt = now
                    )
                    entitiesToUpdate.add(entity)
                    syncedCount++
                }
                // Skip unsynced local images to preserve local changes
            }

            // 3. Batch insert all entities at once
            if (entitiesToUpdate.isNotEmpty()) {
                dao.insertCustomerImages(entitiesToUpdate)
                CustomerLogger.d("CustomerImageSync", "Batch updated ${entitiesToUpdate.size} entities")
            }

            // 3. Update last sync time
            if (serverImages.isNotEmpty()) {
                val maxServerTime = serverImages.mapNotNull { it.updatedAt }.maxOrNull()
                if (maxServerTime != null) {
                    appPreferences.setCustomerLastSyncTime(maxServerTime)
                }
            }

            CustomerLogger.i("CustomerImageSync", "Customer image sync completed. Synced: $syncedCount images")
            Result.success(syncedCount)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageSync", "Customer image sync failed", e)
            Result.failure(e)
        }
    }

    // Helper methods

    /**
     * Clean up stale UPLOADING records for all customers that are older than the specified timeout.
     * This should be called on app startup to handle cases where the app crashed during upload operations.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun cleanupAllStaleUploads(timeoutMinutes: Int = 5) {
        try {
            val now = Clock.System.now()
            val timeoutThreshold = now.minus(kotlin.time.Duration.parse("${timeoutMinutes}m"))
            val thresholdString = timeoutThreshold.toString()

            CustomerLogger.i("CustomerImageRepository", "Starting cleanup of all stale UPLOADING records older than $timeoutMinutes minutes")

            // Find all UPLOADING records that are older than the timeout
            val allUploadingImages = dao.getUnsyncedCustomerImages()
                .filter { entity ->
                    entity.uploadStatus == CustomerImageStatus.UPLOADING &&
                    entity.localUpdatedAt < thresholdString
                }

            if (allUploadingImages.isNotEmpty()) {
                CustomerLogger.w("CustomerImageRepository", "Found ${allUploadingImages.size} stale UPLOADING records to clean up")

                // Mark them as FAILED so they can be retried
                allUploadingImages.forEach { entity ->
                    CustomerLogger.w("CustomerImageRepository", "Marking stale UPLOADING record as FAILED: ${entity.uid} (customer: ${entity.customerId})")
                    dao.updateUploadStatus(entity.uid, CustomerImageStatus.FAILED, now.toString())
                }

                CustomerLogger.i("CustomerImageRepository", "Cleaned up ${allUploadingImages.size} stale UPLOADING records")
            } else {
                CustomerLogger.d("CustomerImageRepository", "No stale UPLOADING records found")
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Error during global stale upload cleanup", e)
        }
    }

    /**
     * Clean up stale UPLOADING records that are older than the specified timeout.
     * This handles cases where the app crashed or was killed during upload operations.
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun cleanupStaleUploadingRecords(customerId: String, timeoutMinutes: Int = 5) {
        try {
            val now = Clock.System.now()
            val timeoutThreshold = now.minus(kotlin.time.Duration.parse("${timeoutMinutes}m"))
            val thresholdString = timeoutThreshold.toString()

            CustomerLogger.d("CustomerImageSync", "Cleaning up UPLOADING records older than $timeoutMinutes minutes (before $thresholdString)")

            // Find all UPLOADING records for this customer that are older than the timeout
            val allUploadingImages = dao.getUnsyncedCustomerImages()
                .filter { entity ->
                    entity.customerId == customerId &&
                    entity.uploadStatus == CustomerImageStatus.UPLOADING &&
                    entity.localUpdatedAt < thresholdString
                }

            if (allUploadingImages.isNotEmpty()) {
                CustomerLogger.w("CustomerImageSync", "Found ${allUploadingImages.size} stale UPLOADING records to clean up")

                // Mark them as FAILED so they can be retried
                allUploadingImages.forEach { entity ->
                    CustomerLogger.w("CustomerImageSync", "Marking stale UPLOADING record as FAILED: ${entity.uid}")
                    dao.updateUploadStatus(entity.uid, CustomerImageStatus.FAILED, now.toString())
                }
            } else {
                CustomerLogger.d("CustomerImageSync", "No stale UPLOADING records found")
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageSync", "Error during stale upload cleanup", e)
        }
    }

    private suspend fun getNextSortOrder(customerId: String): Int {
        val existingImages = dao.getCustomerImages(customerId)
        return (existingImages.maxOfOrNull { it.sortOrder } ?: 0) + 1
    }

    suspend fun getImageCount(customerId: String): Int {
        return dao.getCustomerImageCount(customerId)
    }

    suspend fun getUnsyncedCount(): Int {
        return dao.getUnsyncedCount()
    }

    suspend fun getPendingUploadCount(): Int {
        return dao.getPendingUploadCount()
    }

    suspend fun searchImages(customerId: String, query: String): List<CustomerImageListItem> {
        return dao.searchCustomerImages(customerId, query)
            .map { it.toListItem() }
    }

    suspend fun deleteAllImages(customerId: String): Result<Unit> {
        return try {
            // Get all images for cleanup
            val images = dao.getCustomerImages(customerId)

            // Delete local files
            images.forEach { entity ->
                entity.localPath?.let { localPath ->
                    try {
                        fileManager.deleteFile(localPath)
                    } catch (e: Exception) {
                        CustomerLogger.w("CustomerImageRepository", "Failed to delete local file: $localPath", e)
                    }
                }
            }

            // Delete from local database
            dao.deleteCustomerImagesByCustomer(customerId)

            // Background server delete
            try {
                api.deleteAllCustomerImages(customerId)
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageRepository", "Failed to delete all images from server", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

/**
 * Platform-specific file manager interface for image storage.
 * Implementations should handle platform-specific file operations.
 */
interface PlatformFileManager {
    suspend fun saveImageToCache(imageId: String, imageData: ByteArray, fileName: String): String
    suspend fun deleteFile(filePath: String)
    suspend fun fileExists(filePath: String): Boolean
    suspend fun getFileSize(filePath: String): Long
    suspend fun getCacheDirectory(): String
    suspend fun readFile(filePath: String): ByteArray
}