package com.ampairs.inventory.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.api.InventoryItemApi
import com.ampairs.inventory.data.db.InventoryItemDao
import com.ampairs.inventory.data.db.toEntity
import com.ampairs.inventory.data.db.toInventoryItem
import com.ampairs.inventory.util.InventoryLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/** Owns all inventory-item ↔ server traffic via the canonical /items/sync endpoint (spec 014). */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.INVENTORY)
class InventoryItemSyncDelegate(
    private val api: InventoryItemApi,
    private val dao: InventoryItemDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.INVENTORY

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()

    private suspend fun pushPending(): Result<Int> = try {
        val unsynced = dao.getUnsyncedItems()
        if (unsynced.isEmpty()) {
            Result.success(0)
        } else {
            var syncedCount = 0
            var failedCount = 0
            for (batch in unsynced.chunked(100)) {
                api.bulkUpsertItems(batch.map { it.toInventoryItem() })
                    .onSuccess {
                        batch.forEach { entity ->
                            if (!entity.active) dao.hardDeleteItem(entity.id)
                            else dao.insertItem(entity.copy(synced = true))
                        }
                        syncedCount += batch.size
                    }
                    .onFailure { e ->
                        InventoryLogger.w("InventoryItemSyncDelegate", "Batch upsert failed", e)
                        failedCount += batch.size
                    }
            }
            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount inventory item(s) failed to push"))
            } else {
                Result.success(syncedCount)
            }
        }
    } catch (e: Exception) {
        InventoryLogger.e("InventoryItemSyncDelegate", "Push failed", e)
        Result.failure(e)
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = try {
        var totalSynced = 0
        var currentPage = 0
        do {
            val pageResponse = api.getItems(currentPage, batchSize)
            if (pageResponse.error != null) throw Exception(pageResponse.error?.message ?: "Network error")
            val batch = pageResponse.data?.content ?: emptyList()
            val toInsert = batch.mapNotNull { server ->
                val existing = dao.getItemById(server.uid)
                when {
                    existing != null && !existing.synced -> null            // local unsynced wins
                    !server.active -> { dao.hardDeleteItem(server.uid); null } // server-deleted
                    else -> server.toEntity().copy(synced = true)
                }
            }
            if (toInsert.isNotEmpty()) dao.insertItems(toInsert)
            totalSynced += batch.size
            currentPage++
        } while (batch.size == batchSize && totalSynced < 10000)
        Result.success(totalSynced)
    } catch (e: Exception) {
        InventoryLogger.e("InventoryItemSyncDelegate", "Pull failed", e)
        Result.failure(e)
    }
}
