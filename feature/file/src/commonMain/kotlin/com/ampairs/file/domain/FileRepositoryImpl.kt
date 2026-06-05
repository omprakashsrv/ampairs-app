package com.ampairs.file.domain

import com.ampairs.common.config.ConfigurationManager
import com.ampairs.common.di.AppScope
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FilePickerResult
import com.ampairs.file.api.FileRepository
import com.ampairs.file.api.FileUploadStatus
import com.ampairs.file.data.api.FileApiService
import com.ampairs.file.db.dao.FileDao
import com.ampairs.file.db.entity.FileEntity
import com.ampairs.file.manager.FileManager
import com.ampairs.file.util.FileLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Local-only data access for files/images plus the entity-scoped pull and set-primary calls that
 * the image ViewModels invoke directly. The central-sync multipart push lives in
 * [com.ampairs.file.sync.FileSyncDelegate], not here.
 *
 * Local writes ([saveLocally]/[deleteFile]) mark FILE as PENDING_PUSH so CentralSyncService runs
 * the upload push automatically.
 */
@Inject
@ContributesBinding(AppScope::class)
class FileRepositoryImpl(
    private val dao: FileDao,
    private val api: FileApiService,
    private val fileManager: FileManager,
    private val syncStateDao: SyncStateDao,
) : FileRepository {

    companion object {
        private const val TAG = "FileRepository"
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.FILE, Clock.System.now().toEpochMilliseconds())
    }

    override fun observeFiles(entityType: FileEntityType, entityUid: String): Flow<List<FileItem>> =
        dao.observeByEntity(entityType.backendValue, entityUid).map { list ->
            list.filter { it.softDeleted == 0 }.map { it.toDomain() }
        }

    override fun observePrimaryFile(entityType: FileEntityType, entityUid: String): Flow<FileItem?> =
        dao.observePrimaryByEntity(entityType.backendValue, entityUid).map { it?.toDomain() }

    override suspend fun saveLocally(
        entityType: FileEntityType,
        entityUid: String,
        result: FilePickerResult,
        isPrimary: Boolean,
    ): Result<FileItem> = runCatching {
        val uid = generateFileUid()
        val localPath = fileManager.saveToCache(uid, result.imageData, result.fileName)
        val now = Clock.System.now().toString()
        val entity = FileEntity(
            uid = uid,
            entityType = entityType.backendValue,
            entityUid = entityUid,
            fileName = result.fileName,
            contentType = result.contentType,
            fileSize = result.fileSize,
            isPrimary = if (isPrimary) 1 else 0,
            uploadStatus = FileUploadStatus.PENDING.name,
            localPath = localPath,
            synced = 0,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        markPending()
        FileLogger.d(TAG, "Saved locally: uid=$uid, entity=${entityType.backendValue}/$entityUid")
        entity.toDomain()
    }

    override suspend fun deleteFile(fileUid: String): Result<Unit> = runCatching {
        dao.softDelete(fileUid)
        markPending()
        FileLogger.d(TAG, "Soft deleted: uid=$fileUid")
    }

    override suspend fun setPrimaryFile(
        entityType: FileEntityType,
        entityUid: String,
        fileUid: String,
    ): Result<Unit> = runCatching {
        dao.clearPrimaryStatus(entityType.backendValue, entityUid)
        dao.setPrimary(fileUid)
        // Propagate to server
        api.setPrimaryImage(fileUid).getOrNull()
        FileLogger.d(TAG, "Set primary: uid=$fileUid, entity=${entityType.backendValue}/$entityUid")
    }

    override suspend fun pullFromServer(entityType: FileEntityType, entityUid: String): Result<Int> = runCatching {
        val response = api.getImages(entityType, entityUid, null).getOrThrow()
        val now = Clock.System.now().toString()
        val entities = response.images.map { img ->
            FileEntity(
                uid = img.uid,
                entityType = img.entityType,
                entityUid = img.entityUid,
                fileName = img.originalFilename,
                contentType = img.contentType,
                fileSize = img.fileSize,
                isPrimary = if (img.isPrimary) 1 else 0,
                displayOrder = img.displayOrder,
                imageUrl = img.imageUrl.toAbsoluteUrl(),
                thumbnailUrl = img.thumbnailUrl.toAbsoluteUrl(),
                uploadStatus = FileUploadStatus.COMPLETED.name,
                synced = 1,
                createdAt = now,
                updatedAt = now,
            )
        }
        dao.insertAll(entities)

        // Set reconciliation: permanently delete local images for this entity that the server no
        // longer returns (deleted server-side). Only fully-synced COMPLETED rows are touched, so a
        // pending local upload is never removed.
        val serverUids = response.images.map { it.uid }.toSet()
        dao.getByEntity(entityType.backendValue, entityUid)
            .filter { it.synced == 1 && it.uploadStatus == FileUploadStatus.COMPLETED.name && it.uid !in serverUids }
            .forEach { dao.hardDeleteByUid(it.uid) }

        FileLogger.d(TAG, "Pulled ${entities.size} images for ${entityType.backendValue}/$entityUid")
        entities.size
    }

    private fun generateFileUid(): String {
        val prefix = "FIL"
        val timestamp = Clock.System.now().toEpochMilliseconds().toString()
        val random = (100000..999999).random().toString()
        return "$prefix$timestamp$random"
    }
}

private fun String.toAbsoluteUrl(): String =
    if (startsWith("/")) "${ConfigurationManager.apiBaseUrl}$this" else this

private fun FileEntity.toDomain(): FileItem = FileItem(
    uid = uid,
    entityType = FileEntityType.entries.firstOrNull { it.backendValue == entityType }
        ?: FileEntityType.PRODUCT,
    entityUid = entityUid,
    fileName = fileName,
    contentType = contentType,
    fileSize = fileSize,
    isPrimary = isPrimary == 1,
    displayOrder = displayOrder,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    uploadStatus = runCatching { FileUploadStatus.valueOf(uploadStatus) }.getOrDefault(FileUploadStatus.PENDING),
    localPath = localPath,
    synced = synced == 1,
)
