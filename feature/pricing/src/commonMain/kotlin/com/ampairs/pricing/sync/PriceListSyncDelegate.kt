package com.ampairs.pricing.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.pricing.data.api.PricingApi
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toPriceList
import com.ampairs.pricing.data.db.entity.toPriceListItem
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.util.PricingLogger
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all price-list ↔ server traffic via the unified `/v1/price-lists/sync` contract.
 * Price lists are aggregate-grained: each pushed/pulled [PriceList] carries its `items`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PRICE_LIST)
class PriceListSyncDelegate(
    private val api: PricingApi,
    private val priceListDao: PriceListDao,
    private val itemDao: PriceListItemDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRICE_LIST

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()

    // --- Push -------------------------------------------------------------------------------

    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsyncedHeaders = priceListDao.getUnsyncedPriceLists()
            if (unsyncedHeaders.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0

            for (batch in unsyncedHeaders.chunked(100)) {
                try {
                    val aggregates: List<PriceList> = batch.map { header ->
                        val items = itemDao.getItemsForPriceList(header.id).map { it.toPriceListItem() }
                        header.toPriceList(items)
                    }
                    api.bulkUpdatePriceLists(aggregates)
                    for (header in batch) {
                        if (!header.active) {
                            itemDao.deleteItemsForPriceList(header.id)
                            priceListDao.hardDeletePriceList(header.id)
                        } else {
                            priceListDao.insertPriceList(header.copy(synced = true))
                            val items = itemDao.getItemsForPriceList(header.id)
                            if (items.isNotEmpty()) {
                                itemDao.insertItems(items.map { it.copy(synced = true) })
                            }
                        }
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    PricingLogger.w("PriceListSyncDelegate", "Batch push failed", batchError)
                    failedCount += batch.size
                }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount price list(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            PricingLogger.e("PriceListSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    // --- Pull -------------------------------------------------------------------------------

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.PRICE_LIST) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val page = api.getPriceListsSync(lastSync, currentPage, batchSize, "updatedAt", "ASC")
                val rows = page.content
                for (server in rows) {
                    val existing = priceListDao.getPriceListById(server.uid)
                    when {
                        existing != null && !existing.synced -> Unit // local unsynced edits win
                        !server.active -> {
                            itemDao.deleteItemsForPriceList(server.uid)
                            priceListDao.hardDeletePriceList(server.uid)
                        }
                        else -> {
                            priceListDao.insertPriceList(server.toEntity().copy(synced = true))
                            itemDao.deleteItemsForPriceList(server.uid)
                            if (server.items.isNotEmpty()) {
                                itemDao.insertItems(
                                    server.items.map { it.toEntity(server.uid).copy(synced = true) }
                                )
                            }
                        }
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
                syncStateDao.setLastSyncedAtIso(SyncEntity.PRICE_LIST, maxServerTime)
            }
            Result.success(totalSynced)
        } catch (e: Exception) {
            PricingLogger.e("PriceListSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }
}
