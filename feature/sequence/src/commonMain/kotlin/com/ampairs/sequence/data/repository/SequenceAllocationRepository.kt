package com.ampairs.sequence.data.repository

import com.ampairs.sequence.data.api.SequenceApi
import com.ampairs.sequence.data.db.dao.SequenceAllocationDao
import com.ampairs.sequence.data.db.entity.SequenceAllocationEntity
import com.ampairs.sequence.data.db.entity.toEntity
import com.ampairs.sequence.domain.model.AllocationStatus
import com.ampairs.sequence.domain.model.SequenceAllocationRequest
import com.ampairs.sequence.util.SequenceLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Device block allocations. Consumption is local-only (Room write + PENDING_PUSH flag; the
 * consumption report is pushed by SequenceSyncDelegate).
 *
 * Allowed API exception (offline-sync rules / research R6): [requestAllocation] calls the
 * server directly. A block grant is a UI-invoked, non-sync server operation with no
 * central-sync equivalent — a device must receive its exclusive range synchronously from
 * the server, never via a pull feed.
 */
@OptIn(ExperimentalTime::class)
@Inject
class SequenceAllocationRepository(
    private val allocationDao: SequenceAllocationDao,
    private val sequenceApi: SequenceApi,
    private val syncStateDao: SyncStateDao,
) {

    suspend fun getUsableAllocation(entityType: String, deviceId: String): SequenceAllocationEntity? =
        allocationDao.getUsableAllocation(entityType, deviceId)

    /**
     * Consumes one value from the block: advances next_available, flips to EXHAUSTED past the
     * end, marks the row unsynced and flags SEQUENCE for push. Caller must serialize calls
     * (SequenceNumberProvider holds the mutex).
     */
    suspend fun consume(allocation: SequenceAllocationEntity): Result<Long> {
        return try {
            val value = allocation.nextAvailable
            val newNext = value + allocation.incrementStep
            allocationDao.insertAllocation(
                allocation.copy(
                    nextAvailable = newNext,
                    status = if (newNext > allocation.rangeEnd) AllocationStatus.EXHAUSTED else allocation.status,
                    synced = false,
                )
            )
            markPending()
            Result.success(value)
        } catch (e: Exception) {
            SequenceLogger.e("SequenceAllocationRepository", "Failed to consume from block ${allocation.uid}", e)
            Result.failure(e)
        }
    }

    /** Requests a new exclusive block from the server and stores it locally. */
    suspend fun requestAllocation(
        entityType: String,
        deviceId: String,
        blockSize: Int,
    ): Result<SequenceAllocationEntity> {
        return try {
            val granted = sequenceApi.requestAllocation(
                SequenceAllocationRequest(entityType = entityType, deviceId = deviceId, blockSize = blockSize)
            )
            val entity = granted.toEntity(synced = true)
            allocationDao.insertAllocation(entity)
            SequenceLogger.i(
                "SequenceAllocationRepository",
                "Granted block ${granted.rangeStart}..${granted.rangeEnd} for $entityType"
            )
            Result.success(entity)
        } catch (e: Exception) {
            SequenceLogger.w("SequenceAllocationRepository", "Block request failed for $entityType (offline?)", e)
            Result.failure(e)
        }
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.SEQUENCE, Clock.System.now().toEpochMilliseconds())
    }
}
