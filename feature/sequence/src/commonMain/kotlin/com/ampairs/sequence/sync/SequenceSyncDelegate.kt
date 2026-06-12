package com.ampairs.sequence.sync

import com.ampairs.common.DeviceService
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sequence.data.api.SequenceApi
import com.ampairs.sequence.data.db.dao.SequenceAllocationDao
import com.ampairs.sequence.data.db.dao.SequenceDefinitionDao
import com.ampairs.sequence.data.db.entity.toDefinition
import com.ampairs.sequence.data.db.entity.toEntity
import com.ampairs.sequence.domain.model.AllocationStatus
import com.ampairs.sequence.domain.model.SequenceAllocationReportRequest
import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.util.SequenceLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all sequence ↔ server traffic. Repositories are local-only; this delegate is the single
 * sync-path holder of [SequenceApi]:
 *  - PULL: definitions via the canonical `/v1/definitions/sync` feed (inactive rows hard-deleted
 *    locally, local unsynced edits win) + recovery of this device's ACTIVE allocations.
 *  - PUSH: unsynced definition edits (bulk, deactivations in-band) + allocation consumption
 *    reports (`/v1/allocations/report`).
 *
 * The on-demand block grant lives in SequenceAllocationRepository — a UI-invoked, non-sync
 * RPC (allowed exception, research R6).
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.SEQUENCE)
class SequenceSyncDelegate(
    private val sequenceApi: SequenceApi,
    private val definitionDao: SequenceDefinitionDao,
    private val allocationDao: SequenceAllocationDao,
    private val syncStateDao: SyncStateDao,
    private val deviceService: DeviceService,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.SEQUENCE

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
     * Pushes unsynced definition edits (bulk upsert; deactivated rows in-band, hard-deleted
     * locally once confirmed) and unsynced allocation consumption progress. Either leg failing
     * while nothing succeeded yields a failure so CentralSyncService retries on reconnect.
     */
    private suspend fun pushPending(): Result<Int> {
        return try {
            var syncedCount = 0
            var failedCount = 0

            val unsyncedDefinitions = definitionDao.getUnsyncedDefinitions()
            for (batch in unsyncedDefinitions.chunked(100)) {
                try {
                    sequenceApi.bulkUpdateDefinitions(batch.map { it.toDefinition() })
                    for (entity in batch) {
                        if (!entity.active) {
                            definitionDao.hardDeleteDefinition(entity.uid)
                        } else {
                            definitionDao.insertDefinition(entity.copy(synced = true))
                        }
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    SequenceLogger.w("SequenceSyncDelegate", "Definition batch push failed", batchError)
                    failedCount += batch.size
                }
            }

            val unsyncedAllocations = allocationDao.getUnsyncedAllocations()
            if (unsyncedAllocations.isNotEmpty()) {
                try {
                    val reports = unsyncedAllocations.map {
                        SequenceAllocationReportRequest(uid = it.uid, nextAvailable = it.nextAvailable)
                    }
                    val serverState = sequenceApi.reportAllocations(reports)
                    val serverByUid = serverState.associateBy { it.uid }
                    for (entity in unsyncedAllocations) {
                        // Keep local progress (it can be ahead of a stale server echo); only
                        // the synced flag is reconciled. Server-side status wins when exhausted.
                        val server = serverByUid[entity.uid]
                        val status = if (server?.status == AllocationStatus.EXHAUSTED) {
                            AllocationStatus.EXHAUSTED
                        } else {
                            entity.status
                        }
                        allocationDao.insertAllocation(entity.copy(status = status, synced = true))
                    }
                    syncedCount += unsyncedAllocations.size
                } catch (reportError: Exception) {
                    SequenceLogger.w("SequenceSyncDelegate", "Allocation report failed", reportError)
                    failedCount += unsyncedAllocations.size
                }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount sequence record(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            SequenceLogger.e("SequenceSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    // --- Pull -------------------------------------------------------------------------------

    /**
     * Batched incremental pull of definitions (local unsynced edits win; server-inactive rows
     * hard-deleted locally), then recovery of this device's ACTIVE allocations (rows the server
     * knows about but the local DB lost — e.g. after reinstall). Local allocation progress is
     * never overwritten.
     */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.SEQUENCE) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val pageResponse = sequenceApi.getDefinitionsSync(
                    lastSync,
                    currentPage,
                    batchSize,
                    "updatedAt",
                    "ASC",
                )

                val batchDefinitions = pageResponse.content
                if (batchDefinitions.isNotEmpty()) {
                    val entities = batchDefinitions.mapNotNull { serverDefinition ->
                        val existing = definitionDao.getByUid(serverDefinition.uid)
                        when {
                            existing != null && !existing.synced -> null // local unsynced edits win
                            !serverDefinition.active -> {
                                definitionDao.hardDeleteDefinition(serverDefinition.uid)
                                null
                            }
                            else -> serverDefinition.toEntity().copy(synced = true)
                        }
                    }
                    if (entities.isNotEmpty()) definitionDao.insertDefinitions(entities)

                    val batchMaxTime = getMaxUpdatedAt(batchDefinitions)
                    if (batchMaxTime > maxServerTime) maxServerTime = batchMaxTime

                    totalSynced += batchDefinitions.size
                }

                currentPage++
            } while (pageResponse.hasNext && totalSynced < 10000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.SEQUENCE, maxServerTime)
            }

            totalSynced += recoverDeviceAllocations()

            Result.success(totalSynced)
        } catch (e: Exception) {
            SequenceLogger.e("SequenceSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }

    private suspend fun recoverDeviceAllocations(): Int {
        return try {
            val deviceId = deviceService.getDeviceId()
            if (deviceId.isBlank()) return 0
            val serverAllocations = sequenceApi.getDeviceAllocations(deviceId, AllocationStatus.ACTIVE)
            var recovered = 0
            for (allocation in serverAllocations) {
                if (allocationDao.getByUid(allocation.uid) == null) {
                    allocationDao.insertAllocation(allocation.toEntity(synced = true))
                    recovered++
                }
            }
            recovered
        } catch (e: Exception) {
            SequenceLogger.w("SequenceSyncDelegate", "Allocation recovery skipped", e)
            0
        }
    }

    // --- Helpers ----------------------------------------------------------------------------

    private fun getMaxUpdatedAt(serverDefinitions: List<SequenceDefinition>): String =
        serverDefinitions.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
}
