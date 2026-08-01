package com.ampairs.inventory.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.api.InventoryTransactionApi
import com.ampairs.inventory.data.db.InventoryTransactionDao
import com.ampairs.inventory.data.db.toEntity
import com.ampairs.inventory.data.db.toMovement
import com.ampairs.inventory.util.InventoryLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all inventory-movement ↔ server traffic via /transactions/sync (spec 014).
 * Append-only: push only creates; pull only upserts (movements are never deleted). Items must push
 * before movements (a movement references an item), hence [pushDependencies] = INVENTORY.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.INVENTORY_TRANSACTION)
class InventoryTransactionSyncDelegate(
    private val api: InventoryTransactionApi,
    private val dao: InventoryTransactionDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.INVENTORY_TRANSACTION

    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.INVENTORY)

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()

    private suspend fun pushPending(): Result<Int> = try {
        val unsynced = dao.getUnsyncedMovements()
        if (unsynced.isEmpty()) {
            Result.success(0)
        } else {
            var syncedCount = 0
            var failedCount = 0
            for (batch in unsynced.chunked(100)) {
                api.bulkUpsertMovements(batch.map { it.toMovement() })
                    .onSuccess { server ->
                        // Replace local rows with the server-resolved ones (balance_after, number).
                        val byId = server.associateBy { it.uid }
                        batch.forEach { entity ->
                            val resolved = byId[entity.id]?.toEntity() ?: entity
                            dao.insertMovement(resolved.copy(synced = true))
                        }
                        syncedCount += batch.size
                    }
                    .onFailure { e ->
                        InventoryLogger.w("InventoryTransactionSyncDelegate", "Batch upsert failed", e)
                        failedCount += batch.size
                    }
            }
            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount movement(s) failed to push"))
            } else {
                Result.success(syncedCount)
            }
        }
    } catch (e: Exception) {
        InventoryLogger.e("InventoryTransactionSyncDelegate", "Push failed", e)
        Result.failure(e)
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = try {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.INVENTORY_TRANSACTION) ?: ""
        var totalSynced = 0
        var currentPage = 0
        var maxTime = ""
        do {
            val pageResponse = api.getMovements(currentPage, batchSize, lastSync.ifBlank { null })
            if (pageResponse.error != null) throw Exception(pageResponse.error?.message ?: "Network error")
            val batch = pageResponse.data?.content ?: emptyList()
            val toInsert = batch.mapNotNull { server ->
                val existing = dao.getMovementById(server.uid)
                if (existing != null && !existing.synced) null  // local unsynced wins
                else server.toEntity().copy(synced = true)
            }
            if (toInsert.isNotEmpty()) dao.insertMovements(toInsert)
            val batchMax = batch.mapNotNull { it.updatedAt?.takeIf { s -> s.isNotBlank() } }.maxOrNull() ?: ""
            if (batchMax > maxTime) maxTime = batchMax
            totalSynced += batch.size
            currentPage++
        } while (batch.size == batchSize && totalSynced < 10000)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.INVENTORY_TRANSACTION, maxTime)
        Result.success(totalSynced)
    } catch (e: Exception) {
        InventoryLogger.e("InventoryTransactionSyncDelegate", "Pull failed", e)
        Result.failure(e)
    }
}
