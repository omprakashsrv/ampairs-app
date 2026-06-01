package com.ampairs.product.data.repository

import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.product.data.api.ProductImageApi
import com.ampairs.product.db.dao.ProductUploadImageDao
import com.ampairs.product.db.entity.ProductUploadImageEntity
import com.ampairs.product.db.entity.toEntity
import com.ampairs.product.db.entity.toListItem
import com.ampairs.product.db.entity.toProductUploadImage
import com.ampairs.product.domain.ProductImageStatus
import com.ampairs.product.domain.ProductUploadImage
import com.ampairs.product.domain.ProductUploadImageListItem
import com.ampairs.product.util.ProductLogger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "ProductImageRepository"
private const val PRODUCT_IMAGE_UID_PREFIX = "PMI"

@Inject
class ProductImageRepository(
    private val dao: ProductUploadImageDao,
    private val api: ProductImageApi,
    private val fileManager: ProductFileManager,
) {

    fun observeProductImages(productId: String): Flow<List<ProductUploadImageListItem>> {
        return dao.observeProductImages(productId)
            .map { entities ->
                entities.map { it.toListItem() }
                    .sortedWith(compareByDescending<ProductUploadImageListItem> { it.isPrimary }.thenBy { it.sortOrder })
            }
    }

    suspend fun getProductImage(uid: String): ProductUploadImage? {
        return dao.getProductImage(uid)?.toProductUploadImage()
    }

    /**
     * Saves the image to local cache and DB with PENDING status.
     * The caller is responsible for triggering the actual upload via CentralSyncService.markPendingPush(PRODUCT_IMAGE).
     */
    @OptIn(ExperimentalTime::class)
    suspend fun saveImageLocally(
        productId: String,
        fileName: String,
        contentType: String,
        fileSize: Long,
        imageData: ByteArray,
        description: String? = null,
        isPrimary: Boolean = false,
    ): Result<ProductUploadImage> {
        val uid = UidGenerator.generateUid(PRODUCT_IMAGE_UID_PREFIX)
        val now = Clock.System.now().toString()
        val sortOrder = getNextSortOrder(productId)

        val localPath = try {
            fileManager.saveImageToCache(uid, imageData, fileName)
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Failed to save image locally: $fileName", e)
            return Result.failure(e)
        }

        val productImage = ProductUploadImage(
            uid = uid,
            productId = productId,
            fileName = fileName,
            contentType = contentType,
            fileSize = fileSize,
            description = description,
            isPrimary = isPrimary,
            sortOrder = sortOrder,
            uploadStatus = ProductImageStatus.PENDING,
            localPath = localPath,
        )

        if (isPrimary) {
            dao.setPrimaryImageAtomic(productId, uid, now)
        }

        dao.insertProductImage(productImage.toEntity(synced = false, localCreatedAt = now, localUpdatedAt = now))
        return Result.success(productImage)
    }

    /** Push all PENDING/FAILED product images to the server. Called by CentralSyncService via delegate. */
    @OptIn(ExperimentalTime::class)
    suspend fun pushPendingToServer(): Result<Int> {
        return try {
            ProductLogger.i(TAG, "Pushing all pending product images")
            cleanupAllStaleUploads()

            val unsyncedImages = dao.getUnsyncedImages()
            if (unsyncedImages.isEmpty()) return Result.success(0)

            val now = Clock.System.now().toString()
            var syncedCount = 0
            val entitiesToUpdate = mutableListOf<ProductUploadImageEntity>()

            for (entity in unsyncedImages) {
                if (entity.uploadStatus != ProductImageStatus.PENDING && entity.uploadStatus != ProductImageStatus.FAILED) continue
                val localPath = entity.localPath
                if (localPath != null && fileManager.fileExists(localPath)) {
                    try {
                        withTimeout(130_000L) {
                            val imageData = fileManager.readFile(localPath)
                            val uploadResponse = api.uploadProductImageMultipart(
                                uid = entity.uid,
                                productId = entity.productId,
                                fileName = entity.fileName,
                                contentType = entity.contentType,
                                imageData = imageData,
                                description = entity.description,
                                isPrimary = entity.isPrimary,
                                displayOrder = entity.sortOrder,
                            )
                            val syncedImage = entity.toProductUploadImage().copy(
                                imageUrl = uploadResponse.imageUrl,
                                thumbnailUrl = uploadResponse.thumbnailUrl,
                                uploadStatus = ProductImageStatus.COMPLETED,
                            )
                            entitiesToUpdate.add(syncedImage.toEntity(synced = true, localCreatedAt = entity.localCreatedAt, localUpdatedAt = now))
                            syncedCount++
                            ProductLogger.i(TAG, "Pushed image: ${entity.uid}")
                        }
                    } catch (e: Exception) {
                        ProductLogger.e(TAG, "Failed to push image: ${entity.uid}", e)
                        entitiesToUpdate.add(entity.copy(uploadStatus = ProductImageStatus.FAILED, localUpdatedAt = now))
                    }
                } else {
                    ProductLogger.w(TAG, "No local file for: ${entity.uid} — marking FAILED")
                    entitiesToUpdate.add(entity.copy(uploadStatus = ProductImageStatus.FAILED, localUpdatedAt = now))
                }
            }

            if (entitiesToUpdate.isNotEmpty()) dao.insertProductImages(entitiesToUpdate)
            ProductLogger.i(TAG, "Push complete. Pushed: $syncedCount images")
            if (syncedCount == 0 && entitiesToUpdate.any { it.uploadStatus == ProductImageStatus.FAILED }) {
                Result.failure(Exception("Failed to upload pending images — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Push failed", e)
            Result.failure(e)
        }
    }

    /** Pull all product images from the server for every product known locally. Called by CentralSyncService via delegate. */
    @OptIn(ExperimentalTime::class)
    suspend fun pullFromServer(): Result<Int> {
        return try {
            val productIds = dao.getDistinctProductIds()
            var totalSynced = 0
            val now = Clock.System.now().toString()

            for (productId in productIds) {
                try {
                    val serverImages = api.getProductImages(productId, "")
                    val existingMap = dao.getProductImages(productId).associateBy { it.uid }
                    val entitiesToUpdate = mutableListOf<ProductUploadImageEntity>()

                    for (serverImage in serverImages) {
                        val existing = existingMap[serverImage.uid]
                        when {
                            existing == null -> {
                                entitiesToUpdate.add(
                                    serverImage.copy(uploadStatus = ProductImageStatus.COMPLETED, localPath = null)
                                        .toEntity(synced = true, localCreatedAt = now, localUpdatedAt = now)
                                )
                                totalSynced++
                            }
                            existing.synced -> {
                                val merged = serverImage.copy(uploadStatus = existing.uploadStatus, localPath = existing.localPath)
                                entitiesToUpdate.add(merged.toEntity(synced = true, localCreatedAt = existing.localCreatedAt, localUpdatedAt = now))
                                totalSynced++
                            }
                        }
                    }

                    if (entitiesToUpdate.isNotEmpty()) dao.insertProductImages(entitiesToUpdate)
                } catch (e: Exception) {
                    ProductLogger.e(TAG, "Failed to pull for product: $productId", e)
                }
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Pull failed", e)
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun deleteImage(uid: String): Result<Unit> {
        val existing = dao.getProductImage(uid)
            ?: return Result.failure(Exception("Image not found"))

        dao.deleteProductImage(uid)

        existing.localPath?.let { path ->
            try { fileManager.deleteFile(path) } catch (e: Exception) {
                ProductLogger.w(TAG, "Failed to delete local file: $path", e)
            }
        }

        try {
            api.deleteProductImage(existing.productId, uid)
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Failed to delete image from server: $uid", e)
        }

        return Result.success(Unit)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun setPrimaryImage(uid: String): Result<ProductUploadImage> {
        val existing = dao.getProductImage(uid)
            ?: return Result.failure(Exception("Image not found"))

        val now = Clock.System.now().toString()

        if (existing.isPrimary) return Result.success(existing.toProductUploadImage())

        dao.setPrimaryImageAtomic(existing.productId, uid, now)
        val updatedImage = existing.toProductUploadImage().copy(isPrimary = true)

        return try {
            val serverImage = api.setPrimaryImage(existing.productId, uid)
            val mergedImage = serverImage.copy(localPath = existing.localPath, uploadStatus = existing.uploadStatus)
            dao.insertProductImage(mergedImage.toEntity(synced = true, localCreatedAt = existing.localCreatedAt, localUpdatedAt = now))
            Result.success(mergedImage.copy(isPrimary = true))
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Failed to sync primary image to server", e)
            Result.success(updatedImage)
        }
    }

    /** Full sync for a specific product — used by delegate.handleBackendEvent(). */
    @OptIn(ExperimentalTime::class)
    suspend fun syncProductImages(productId: String): Result<Int> {
        return try {
            ProductLogger.i(TAG, "Full sync for product: $productId")

            val now = Clock.System.now().toString()
            var syncedCount = 0
            val entitiesToUpdate = mutableListOf<ProductUploadImageEntity>()

            cleanupStaleUploadingRecords(productId)

            val unsyncedImages = dao.getUnsyncedImages().filter { it.productId == productId }
            for (entity in unsyncedImages) {
                if (entity.uploadStatus != ProductImageStatus.PENDING && entity.uploadStatus != ProductImageStatus.FAILED) continue
                val localPath = entity.localPath
                if (localPath != null && fileManager.fileExists(localPath)) {
                    try {
                        withTimeout(130_000L) {
                            val imageData = fileManager.readFile(localPath)
                            val uploadResponse = api.uploadProductImageMultipart(
                                uid = entity.uid,
                                productId = entity.productId,
                                fileName = entity.fileName,
                                contentType = entity.contentType,
                                imageData = imageData,
                                description = entity.description,
                                isPrimary = entity.isPrimary,
                                displayOrder = entity.sortOrder,
                            )
                            val syncedImage = entity.toProductUploadImage().copy(
                                imageUrl = uploadResponse.imageUrl,
                                thumbnailUrl = uploadResponse.thumbnailUrl,
                                uploadStatus = ProductImageStatus.COMPLETED,
                            )
                            entitiesToUpdate.add(syncedImage.toEntity(synced = true, localCreatedAt = entity.localCreatedAt, localUpdatedAt = now))
                            syncedCount++
                        }
                    } catch (e: Exception) {
                        ProductLogger.e(TAG, "Failed to push image: ${entity.uid}", e)
                        entitiesToUpdate.add(entity.copy(uploadStatus = ProductImageStatus.FAILED, localUpdatedAt = now))
                    }
                } else {
                    entitiesToUpdate.add(entity.copy(uploadStatus = ProductImageStatus.FAILED, localUpdatedAt = now))
                }
            }

            val serverImages = try {
                api.getProductImages(productId, "")
            } catch (e: Exception) {
                ProductLogger.e(TAG, "Failed to fetch server images for product: $productId", e)
                emptyList()
            }
            val existingMap = dao.getProductImages(productId).associateBy { it.uid }
            for (serverImage in serverImages) {
                val existing = existingMap[serverImage.uid]
                when {
                    existing == null -> {
                        entitiesToUpdate.add(serverImage.copy(uploadStatus = ProductImageStatus.COMPLETED, localPath = null)
                            .toEntity(synced = true, localCreatedAt = now, localUpdatedAt = now))
                        syncedCount++
                    }
                    existing.synced -> {
                        val merged = serverImage.copy(uploadStatus = existing.uploadStatus, localPath = existing.localPath)
                        entitiesToUpdate.add(merged.toEntity(synced = true, localCreatedAt = existing.localCreatedAt, localUpdatedAt = now))
                        syncedCount++
                    }
                }
            }

            if (entitiesToUpdate.isNotEmpty()) dao.insertProductImages(entitiesToUpdate)
            Result.success(syncedCount)
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Sync failed for product: $productId", e)
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun cleanupAllStaleUploads(timeoutMinutes: Int = 5) {
        try {
            val now = Clock.System.now()
            val threshold = now.minus(kotlin.time.Duration.parse("${timeoutMinutes}m")).toString()
            val stale = dao.getUnsyncedImages().filter { entity ->
                entity.uploadStatus == ProductImageStatus.UPLOADING && entity.localUpdatedAt < threshold
            }
            if (stale.isNotEmpty()) {
                ProductLogger.w(TAG, "Cleaning up ${stale.size} stale UPLOADING records")
                stale.forEach { dao.updateUploadStatus(it.uid, ProductImageStatus.FAILED, now.toString()) }
            }
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error during stale upload cleanup", e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun cleanupStaleUploadingRecords(productId: String, timeoutMinutes: Int = 5) {
        try {
            val now = Clock.System.now()
            val threshold = now.minus(kotlin.time.Duration.parse("${timeoutMinutes}m")).toString()
            val stale = dao.getUnsyncedImages().filter { entity ->
                entity.productId == productId &&
                entity.uploadStatus == ProductImageStatus.UPLOADING &&
                entity.localUpdatedAt < threshold
            }
            if (stale.isNotEmpty()) {
                ProductLogger.w(TAG, "Cleaning up ${stale.size} stale UPLOADING records for product: $productId")
                stale.forEach { dao.updateUploadStatus(it.uid, ProductImageStatus.FAILED, now.toString()) }
            }
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error during stale upload cleanup", e)
        }
    }

    private suspend fun getNextSortOrder(productId: String): Int {
        val existing = dao.getProductImages(productId)
        return (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
    }
}
