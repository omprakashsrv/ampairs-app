package com.ampairs.unit.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.unit.data.api.UnitApi
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.db.entity.toEntity
import com.ampairs.unit.data.db.entity.toUnit
import com.ampairs.unit.util.UnitLogger
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all unit ↔ server traffic. The repository is local-only; this delegate is the single place
 * that talks to [UnitApi] — bulk push of unsynced rows, batched pull, and backend event refresh.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.UNIT)
class UnitSyncDelegate(
    private val unitApi: UnitApi,
    private val unitDao: UnitDao,
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

    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = unitDao.getUnsyncedUnits()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0
            for (entity in unsynced) {
                val unit = entity.toUnit()
                try {
                    if (!entity.active) {
                        unitApi.deleteUnit(unit.uid)
                        unitDao.hardDeleteUnit(unit.uid)
                        syncedCount++
                    } else {
                        val response = unitApi.createUnit(unit)
                        if (response.data != null && response.error == null) {
                            unitDao.insertUnit(response.data!!.toEntity().copy(synced = true))
                            syncedCount++
                        } else {
                            failedCount++
                        }
                    }
                } catch (e: Exception) {
                    UnitLogger.w("UnitSyncDelegate", "Failed to push unit ${unit.uid}", e)
                    failedCount++
                }
            }
            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount unit(s) failed to push"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            UnitLogger.e("UnitSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            var totalSynced = 0
            var currentPage = 0

            do {
                val pageResponse = unitApi.getUnits(currentPage, batchSize)
                if (pageResponse.error != null) throw Exception(pageResponse.error?.message ?: "Network error")
                val batchUnits = pageResponse.data?.content ?: emptyList()

                val unitsToInsert = batchUnits.mapNotNull { serverUnit ->
                    val existing = unitDao.getUnitById(serverUnit.uid)
                    if (existing != null && !existing.synced) null
                    else serverUnit.toEntity().copy(synced = true)
                }

                if (unitsToInsert.isNotEmpty()) {
                    unitDao.insertUnits(unitsToInsert)
                    totalSynced += unitsToInsert.size
                }

                currentPage++
            } while (batchUnits.size == batchSize && totalSynced < 10000)

            Result.success(totalSynced)
        } catch (e: Exception) {
            UnitLogger.e("UnitSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }

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
}
