package com.ampairs.unit.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.unit.data.api.UnitApi
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.db.entity.toEntity
import com.ampairs.unit.data.db.entity.toUnit
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.util.UnitLogger
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all unit ↔ server traffic. The repository is local-only; this delegate is the single place
 * that talks to [UnitApi] via the unified `/v1/units/sync` contract — bulk push of unsynced rows
 * (soft-deletes sent in-band), batched incremental pull, and backend event refresh.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.UNIT)
class UnitSyncDelegate(
    private val unitApi: UnitApi,
    private val unitDao: UnitDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.UNIT

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
        runCatching { handleExternalEvent(entityId) }.fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )

    // --- Push -------------------------------------------------------------------------------

    /**
     * Bulk push all locally unsynced rows through the unified `/sync` endpoint. Soft-deleted rows
     * (active = false) are sent IN-BAND in the same bulk body so the server can mark them DELETED;
     * there is no separate per-row DELETE call. After a successful push, soft-deleted rows are
     * hard-deleted locally and active rows are marked synced = true.
     */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = unitDao.getUnsyncedUnits()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0

            for (batch in unsynced.chunked(100)) {
                try {
                    val payload = batch.map { it.toUnit() }
                    unitApi.bulkUpdateUnits(payload)
                    // Push succeeded — reconcile each row locally.
                    for (entity in batch) {
                        if (!entity.active) {
                            unitDao.hardDeleteUnit(entity.id)
                        } else {
                            unitDao.insertUnit(entity.copy(synced = true))
                        }
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    UnitLogger.w("UnitSyncDelegate", "Batch push failed", batchError)
                    failedCount += batch.size
                }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount unit(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            UnitLogger.e("UnitSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    // --- Pull -------------------------------------------------------------------------------

    /**
     * Batched incremental pull through the unified `/sync` endpoint. Reconciles each server row:
     * local unsynced edits win; server rows that are inactive/DELETED are permanently removed
     * locally; everything else is upserted as synced. Advances the lastSyncedAtIso checkpoint.
     */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.UNIT) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val pageResponse = unitApi.getUnitsSync(
                    lastSync,
                    currentPage,
                    batchSize,
                    "updatedAt",
                    "ASC",
                )

                val batchUnits = pageResponse.content
                if (batchUnits.isNotEmpty()) {
                    val entities = batchUnits.mapNotNull { serverUnit ->
                        val existing = unitDao.getUnitById(serverUnit.uid)
                        when {
                            existing != null && !existing.synced -> {
                                // Don't overwrite unsynced local edits.
                                null
                            }
                            !serverUnit.active -> {
                                unitDao.hardDeleteUnit(serverUnit.uid)
                                null
                            }
                            else -> serverUnit.toEntity().copy(synced = true)
                        }
                    }
                    if (entities.isNotEmpty()) unitDao.insertUnits(entities)

                    val batchMaxTime = getMaxUpdatedAt(batchUnits)
                    if (batchMaxTime > maxServerTime) maxServerTime = batchMaxTime

                    totalSynced += batchUnits.size
                }

                currentPage++
            } while (pageResponse.hasNext && totalSynced < 10000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.UNIT, maxServerTime)
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            UnitLogger.e("UnitSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }

    // --- Backend event ----------------------------------------------------------------------

    private suspend fun handleExternalEvent(unitId: String) {
        try {
            val response = unitApi.getUnitById(unitId)
            if (response.data != null && response.error == null) {
                val existing = unitDao.getUnitById(unitId)
                if (existing == null || existing.synced) {
                    // Don't overwrite unsynced local edits.
                    unitDao.insertUnit(response.data!!.toEntity().copy(synced = true))
                }
            }
        } catch (e: Exception) {
            UnitLogger.w("UnitSyncDelegate", "Failed to handle event for unit $unitId", e)
        }
    }

    // --- Helpers ----------------------------------------------------------------------------

    private fun getMaxUpdatedAt(serverUnits: List<Unit>): String =
        serverUnits.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
}
