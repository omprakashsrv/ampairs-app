package com.ampairs.file.sync

import com.ampairs.common.config.ConfigurationManager
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileUploadStatus
import com.ampairs.file.data.api.FileApiService
import com.ampairs.file.db.dao.FileDao
import com.ampairs.file.manager.FileManager
import com.ampairs.file.util.FileLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns the file/image central-sync push: multipart upload of pending local files and propagation
 * of pending deletes. The [FileApiService] is injected here, not in the repository — the repo only
 * writes locally and flags FILE as PENDING_PUSH, and CentralSyncService drives this push.
 *
 * Pull is entity-scoped (needs a type + uid) and stays a UI-invoked call on the repository, so the
 * central [pullFromServer]/[handleBackendEvent] here are intentionally no-ops.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.FILE)
class FileSyncDelegate(
    private val dao: FileDao,
    private val api: FileApiService,
    private val fileManager: FileManager,
) : SyncDelegate {

    private companion object {
        const val TAG = "FileSyncDelegate"
    }

    override val entity: SyncEntity = SyncEntity.FILE

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pullFromServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        SyncResult.Success(0)

    private suspend fun pushPending(): Result<Int> = runCatching {
        val pending = dao.getPendingUploads()
        val deletes = dao.getPendingDeletes()
        var syncedCount = 0
        var failedCount = 0

        // Push pending deletes first.
        deletes.forEach { entity ->
            api.deleteImage(entity.uid).fold(
                onSuccess = { dao.markSynced(entity.uid); syncedCount++ },
                onFailure = { FileLogger.w(TAG, "Delete failed for ${entity.uid}", it) },
            )
        }

        // Push pending uploads (multipart).
        pending.forEach { entity ->
            val localPath = entity.localPath
            if (localPath == null) {
                FileLogger.w(TAG, "No local path for pending upload uid=${entity.uid}")
                dao.updateUploadStatus(entity.uid, FileUploadStatus.FAILED.name)
                failedCount++
                return@forEach
            }
            val imageData = runCatching { fileManager.readFile(localPath) }.getOrNull()
            if (imageData == null) {
                FileLogger.w(TAG, "Could not read local file: $localPath")
                dao.updateUploadStatus(entity.uid, FileUploadStatus.FAILED.name)
                failedCount++
                return@forEach
            }
            dao.updateUploadStatus(entity.uid, FileUploadStatus.UPLOADING.name)
            val entityType = FileEntityType.entries.firstOrNull { it.backendValue == entity.entityType }
                ?: run {
                    FileLogger.w(TAG, "Unknown entity type: ${entity.entityType}")
                    dao.updateUploadStatus(entity.uid, FileUploadStatus.FAILED.name)
                    failedCount++
                    return@forEach
                }
            api.upload(entityType, entity.entityUid, entity.uid, entity.fileName, entity.contentType, imageData, entity.isPrimary == 1).fold(
                onSuccess = { apiModel ->
                    dao.markUploaded(entity.uid, apiModel.imageUrl.toAbsoluteUrl(), apiModel.thumbnailUrl.toAbsoluteUrl())
                    syncedCount++
                    FileLogger.d(TAG, "Uploaded: uid=${entity.uid}, imageUrl=${apiModel.imageUrl}")
                },
                onFailure = { e ->
                    FileLogger.e(TAG, "Upload failed for ${entity.uid}", e)
                    dao.updateUploadStatus(entity.uid, FileUploadStatus.FAILED.name)
                    failedCount++
                },
            )
        }

        if (syncedCount == 0 && failedCount > 0) {
            error("All $failedCount file uploads failed — will retry on reconnect")
        }
        syncedCount
    }
}

private fun String.toAbsoluteUrl(): String =
    if (startsWith("/")) "${ConfigurationManager.apiBaseUrl}$this" else this
