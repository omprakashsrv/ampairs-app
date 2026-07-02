package com.ampairs.purchase.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.purchase.api.PurchaseApi
import com.ampairs.purchase.api.model.PurchaseApiModel
import com.ampairs.purchase.api.model.asDatabaseModel
import com.ampairs.purchase.api.model.asItemDatabaseModel
import com.ampairs.purchase.api.model.toApiModel
import com.ampairs.purchase.db.dao.PurchaseDao
import com.ampairs.purchase.domain.PurchaseStatus
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns ALL purchase ↔ server traffic. The repository is local-only; this delegate is the single
 * place that talks to [PurchaseApi]. CentralSyncService invokes it on PENDING_PUSH (bulk push),
 * PENDING_PULL (batched pull), and backend WebSocket events.
 *
 * Endpoints target the `/v1/purchases/sync` contract. A purchase references a supplier and products,
 * so those parents push/pull first.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PURCHASE)
class PurchaseSyncDelegate(
    private val purchaseApi: PurchaseApi,
    private val purchaseDao: PurchaseDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PURCHASE

    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.SUPPLIER, SyncEntity.PRODUCT)
    override val dependsOn: List<SyncEntity> = listOf(SyncEntity.SUPPLIER, SyncEntity.PRODUCT)

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

    /** Bulk push all locally unsynced purchases (active upserts AND soft-deleted rows), batched 100. */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = purchaseDao.getUnsyncedPurchases() + purchaseDao.getUnsyncedDeletedPurchases()
            if (unsynced.isEmpty()) return Result.success(0)

            val apiModels = unsynced.map { e ->
                val items = purchaseDao.getPurchaseItems(e.id).toApiModel()
                e.toApiModel(items)
            }

            var synced = 0
            var failed = 0
            for (batch in apiModels.chunked(100)) {
                try {
                    purchaseApi.bulkUpdatePurchases(batch)
                    batch.forEach { api ->
                        if (api.active) {
                            purchaseDao.markAsSynced(api.id)
                        } else {
                            // soft-deleted row is now confirmed on the server — hard-delete locally
                            purchaseDao.deletePurchaseItems(api.id)
                            purchaseDao.deleteById(api.id)
                        }
                    }
                    synced += batch.size
                } catch (e: Exception) {
                    ErrorTracking.captureException(e, "PurchaseSyncDelegate.pushPending")
                    failed += batch.size
                }
            }

            if (synced == 0 && failed > 0) {
                Result.failure(Exception("$failed purchase(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(synced)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Pull -------------------------------------------------------------------------------

    /**
     * Batched incremental pull. Local unsynced edits win; server rows marked deleted/inactive/
     * CANCELLED are removed locally; everything else is upserted (parent + items) as synced.
     * Advances the ISO checkpoint.
     */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.PURCHASE) ?: ""
            var total = 0
            var page = 0
            var maxTime = ""

            do {
                val pageResponse = purchaseApi.getPurchases(lastSync, page, batchSize, "updatedAt", "ASC")
                val content = pageResponse.content
                if (content.isNotEmpty()) {
                    val toUpsert = mutableListOf<PurchaseApiModel>()
                    for (api in content) {
                        val existing = purchaseDao.selectById(api.id)
                        val serverDeleted =
                            api.softDeleted || !api.active || api.status == PurchaseStatus.CANCELLED
                        when {
                            existing != null && existing.synced == 0L -> { /* local unsynced wins */ }
                            serverDeleted -> {
                                purchaseDao.deletePurchaseItems(api.id)
                                purchaseDao.deleteById(api.id)
                            }
                            else -> toUpsert += api
                        }
                    }
                    if (toUpsert.isNotEmpty()) {
                        purchaseDao.upsertPurchases(toUpsert.asDatabaseModel(), toUpsert.asItemDatabaseModel())
                    }
                    val batchMax = content
                        .mapNotNull { it.updatedAt?.takeIf { s -> s.isNotBlank() } }
                        .maxOrNull() ?: ""
                    if (batchMax > maxTime) maxTime = batchMax
                    total += content.size
                }
                page++
            } while (pageResponse.hasNext && total < 10000)

            if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.PURCHASE, maxTime)
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
