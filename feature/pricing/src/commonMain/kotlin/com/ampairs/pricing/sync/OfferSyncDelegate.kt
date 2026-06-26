package com.ampairs.pricing.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.pricing.data.api.PricingApi
import com.ampairs.pricing.data.db.dao.OfferDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toOffer
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.util.PricingLogger
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/** Owns all offer ↔ server traffic via the unified `/v1/offers/sync` contract. */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.OFFER)
class OfferSyncDelegate(
    private val api: PricingApi,
    private val offerDao: OfferDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.OFFER

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()

    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = offerDao.getUnsyncedOffers()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0
            for (batch in unsynced.chunked(100)) {
                try {
                    api.bulkUpdateOffers(batch.map { it.toOffer() })
                    for (o in batch) {
                        if (!o.active) offerDao.hardDeleteOffer(o.id)
                        else offerDao.insertOffer(o.copy(synced = true))
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    PricingLogger.w("OfferSyncDelegate", "Batch push failed", batchError)
                    failedCount += batch.size
                }
            }
            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount offer(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            PricingLogger.e("OfferSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.OFFER) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val page = api.getOffersSync(lastSync, currentPage, batchSize, "updatedAt", "ASC")
                val rows: List<Offer> = page.content
                for (server in rows) {
                    val existing = offerDao.getOfferById(server.uid)
                    when {
                        existing != null && !existing.synced -> Unit
                        !server.active -> offerDao.hardDeleteOffer(server.uid)
                        else -> offerDao.insertOffer(server.toEntity().copy(synced = true))
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
                syncStateDao.setLastSyncedAtIso(SyncEntity.OFFER, maxServerTime)
            }
            Result.success(totalSynced)
        } catch (e: Exception) {
            PricingLogger.e("OfferSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }
}
