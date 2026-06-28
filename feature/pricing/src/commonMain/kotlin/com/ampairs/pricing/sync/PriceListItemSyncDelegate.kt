package com.ampairs.pricing.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.pricing.data.api.PricingApi
import com.ampairs.pricing.data.api.PriceListItemResponse
import com.ampairs.pricing.data.api.toDomain
import com.ampairs.pricing.data.api.toPush
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toPriceListItem
import com.ampairs.pricing.util.PricingLogger
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns price-list ITEM ↔ server traffic via `/v1/price-lists/items/sync`. Money is asymmetric:
 * pulled as MoneyDto, pushed as minor units (see the api-layer wire DTOs). Depends on PRICE_LIST
 * being pushed first so the parent exists server-side.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PRICE_LIST_ITEM)
class PriceListItemSyncDelegate(
    private val api: PricingApi,
    private val itemDao: PriceListItemDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRICE_LIST_ITEM

    /** Items reference a parent price list; push headers first. */
    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.PRICE_LIST)

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()

    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = itemDao.getUnsyncedItems()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0
            for (batch in unsynced.chunked(100)) {
                try {
                    api.bulkUpdatePriceListItems(batch.map { it.toPriceListItem().toPush() })
                    for (item in batch) {
                        if (!item.active) itemDao.hardDeleteItem(item.id)
                        else itemDao.insertItem(item.copy(synced = true))
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    PricingLogger.w("PriceListItemSyncDelegate", "Batch push failed", batchError)
                    failedCount += batch.size
                }
            }
            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount price-list item(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            PricingLogger.e("PriceListItemSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.PRICE_LIST_ITEM) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val page = api.getPriceListItemsSync(lastSync, currentPage, batchSize, "updatedAt", "ASC")
                val rows: List<PriceListItemResponse> = page.content
                for (server in rows) {
                    val existing = itemDao.getItemById(server.uid)
                    when {
                        existing != null && !existing.synced -> Unit // local unsynced edits win
                        !server.active -> itemDao.hardDeleteItem(server.uid)
                        else -> itemDao.insertItem(server.toDomain().toEntity().copy(synced = true))
                    }
                }
                if (rows.isNotEmpty()) {
                    val batchMax = rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
                    if (batchMax > maxServerTime) maxServerTime = batchMax
                    totalSynced += rows.size
                }
                currentPage++
            } while (page.hasNext && totalSynced < 10000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.PRICE_LIST_ITEM, maxServerTime)
            }
            Result.success(totalSynced)
        } catch (e: Exception) {
            PricingLogger.e("PriceListItemSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }
}
