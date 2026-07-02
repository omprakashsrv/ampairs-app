package com.ampairs.notification.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.notification.data.api.NotificationApi
import com.ampairs.notification.data.db.dao.NotificationDao
import com.ampairs.notification.data.db.entity.toAppNotification
import com.ampairs.notification.data.db.entity.toEntity
import com.ampairs.notification.domain.model.AppNotification
import com.ampairs.notification.util.NotificationLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all notification ↔ server traffic. The repository is local-only; this delegate is the single
 * place that talks to [NotificationApi] via the canonical `/v1/notifications/feed/sync` contract —
 * bulk push of unsynced read/dismiss flag changes (dismisses sent in-band), batched incremental
 * pull, and backend-event refresh.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.NOTIFICATION)
class NotificationSyncDelegate(
    private val api: NotificationApi,
    private val dao: NotificationDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.NOTIFICATION

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

    // --- Push -------------------------------------------------------------------------------

    /**
     * Bulk push all locally unsynced rows. Dismissed rows (active = false) are sent IN-BAND in the
     * same body so the server can mark them DELETED; there is no separate per-row DELETE call. After
     * a successful push, dismissed rows are hard-deleted locally and active rows are marked synced.
     */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = dao.getUnsynced()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0

            for (batch in unsynced.chunked(100)) {
                try {
                    val payload = batch.map { it.toAppNotification() }
                    api.pushFeedSync(payload)
                    for (entity in batch) {
                        if (!entity.active) {
                            dao.hardDelete(entity.uid)
                        } else {
                            dao.upsert(entity.copy(synced = true))
                        }
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    NotificationLogger.w("NotificationSyncDelegate", "Batch push failed", batchError)
                    failedCount += batch.size
                }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount notification(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            NotificationLogger.e("NotificationSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    // --- Pull -------------------------------------------------------------------------------

    /**
     * Batched incremental pull. Reconciles each server row: local unsynced edits win; rows the
     * server marks inactive/DELETED are permanently hard-deleted locally; everything else is
     * upserted as synced. Advances the lastSyncedAtIso checkpoint.
     */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.NOTIFICATION) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val pageResponse = api.getFeedSync(
                    lastSync,
                    currentPage,
                    batchSize,
                    "updatedAt",
                    "ASC",
                )

                val batch = pageResponse.content
                if (batch.isNotEmpty()) {
                    val entities = batch.mapNotNull { server ->
                        val existing = dao.getByUid(server.uid)
                        when {
                            existing != null && !existing.synced -> {
                                // Don't overwrite unsynced local flag changes.
                                null
                            }
                            !server.active -> {
                                dao.hardDelete(server.uid)
                                null
                            }
                            else -> server.toEntity().copy(synced = true)
                        }
                    }
                    if (entities.isNotEmpty()) dao.upsertAll(entities)

                    val batchMaxTime = getMaxUpdatedAt(batch)
                    if (batchMaxTime > maxServerTime) maxServerTime = batchMaxTime

                    totalSynced += batch.size
                }

                currentPage++
            } while (pageResponse.hasNext && totalSynced < 10000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.NOTIFICATION, maxServerTime)
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            NotificationLogger.e("NotificationSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }

    // --- Helpers ----------------------------------------------------------------------------

    private fun getMaxUpdatedAt(rows: List<AppNotification>): String =
        rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
}
