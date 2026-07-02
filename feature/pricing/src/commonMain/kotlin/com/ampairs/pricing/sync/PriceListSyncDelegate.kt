package com.ampairs.pricing.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.pricing.data.api.PricingApi
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toPriceList
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.util.PricingLogger
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns price-list HEADER ↔ server traffic via `/v1/price-lists/sync`. Items are a separate feed
 * ([PriceListItemSyncDelegate]) — the backend two-feed contract.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PRICE_LIST)
class PriceListSyncDelegate(
    private val api: PricingApi,
    private val priceListDao: PriceListDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRICE_LIST

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()

    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = priceListDao.getUnsyncedPriceLists()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0
            for (batch in unsynced.chunked(100)) {
                try {
                    api.bulkUpdatePriceLists(batch.map { it.toPriceList() })
                    for (header in batch) {
                        if (!header.active) priceListDao.hardDeletePriceList(header.id)
                        else priceListDao.insertPriceList(header.copy(synced = true))
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

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.PRICE_LIST) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val page = api.getPriceListsSync(lastSync, currentPage, batchSize, "updatedAt", "ASC")
                val rows: List<PriceList> = page.content
                for (server in rows) {
                    val existing = priceListDao.getPriceListById(server.uid)
                    when {
                        existing != null && !existing.synced -> Unit // local unsynced edits win
                        !server.active -> priceListDao.hardDeletePriceList(server.uid)
                        else -> priceListDao.insertPriceList(server.toEntity().copy(synced = true))
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
