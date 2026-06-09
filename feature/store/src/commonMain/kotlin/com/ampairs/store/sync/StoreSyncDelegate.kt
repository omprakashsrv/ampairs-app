package com.ampairs.store.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.store.data.api.StoreSettingApi
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.entity.toEntity
import com.ampairs.store.data.db.entity.toStoreSetting
import com.ampairs.store.util.StoreLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all setting ↔ server traffic. The repository is local-only; this delegate is the single place
 * that talks to [StoreSettingApi] — bulk push of unsynced rows, batched pull, and backend-event refresh.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.STORE)
class StoreSyncDelegate(
    private val api: StoreSettingApi,
    private val dao: StoreSettingDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.STORE

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    /** Bulk push all unsynced rows; on success mark synced (or hard-delete locally inactive rows). */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = dao.getUnsynced()
            if (unsynced.isEmpty()) return Result.success(0)

            val payload = unsynced.map { it.toStoreSetting() }
            val response = api.push(payload)
            if (response.data != null && response.error == null) {
                for (row in unsynced) {
                    if (!row.active) dao.hardDelete(row.id)
                    else dao.insert(row.copy(synced = true))
                }
                Result.success(unsynced.size)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to push settings"))
            }
        } catch (e: Exception) {
            StoreLogger.e("StoreSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    /** Batched pull; local unsynced edits win, server-inactive rows are hard-deleted locally. */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            var totalSynced = 0
            var currentPage = 0

            do {
                val pageResponse = api.pull(currentPage, batchSize)
                if (pageResponse.error != null) throw Exception(pageResponse.error?.message ?: "Network error")
                val batch = pageResponse.data?.content ?: emptyList()

                for (serverSetting in batch) {
                    val existing = dao.getById(serverSetting.uid)
                    // Don't overwrite unsynced local edits.
                    if (existing != null && !existing.synced) continue
                    if (!serverSetting.active) {
                        dao.hardDelete(serverSetting.uid)
                    } else {
                        dao.insert(serverSetting.toEntity().copy(synced = true))
                    }
                }
                totalSynced += batch.size
                currentPage++
            } while (batch.size == batchSize && totalSynced < 10000)

            Result.success(totalSynced)
        } catch (e: Exception) {
            StoreLogger.e("StoreSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }
}
