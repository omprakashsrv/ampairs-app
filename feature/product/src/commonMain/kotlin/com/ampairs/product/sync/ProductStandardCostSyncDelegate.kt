package com.ampairs.product.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.api.model.asStandardCostApiModels
import com.ampairs.product.api.model.toEntity
import com.ampairs.product.data.api.ProductStandardCostApi
import com.ampairs.product.db.dao.ProductStandardCostDao
import com.ampairs.product.db.entity.ProductStandardCostEntity
import com.ampairs.product.util.ProductLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all standard-purchase-cost ↔ server traffic on the canonical `/sync` contract. The
 * resolver/DAO layer is local-only; this delegate is the single place that talks to
 * [ProductStandardCostApi] — bulk push of unsynced rows (active upserts AND in-band soft-deletes),
 * batched incremental pull (permanently deleting server-DELETED/inactive rows; local unsynced edits
 * win), and backend-event refresh. Mirrors [ProductSyncDelegate].
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PRODUCT_STANDARD_COST)
class ProductStandardCostSyncDelegate(
    private val api: ProductStandardCostApi,
    private val dao: ProductStandardCostDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRODUCT_STANDARD_COST

    // Costs reference products — pull/push products first so a cost's product reference resolves.
    override val dependsOn: List<SyncEntity> = listOf(SyncEntity.PRODUCT)
    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.PRODUCT)

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

    private suspend fun pushPending(): Result<Int> = runCatching {
        val unsynced = dao.getUnsynced()
        if (unsynced.isEmpty()) return@runCatching 0
        var pushed = 0
        var failed = 0
        var lastError: Throwable? = null
        // One failed batch must not abort the rest (reference: ProductSyncDelegate) — remaining
        // batches still push; failed rows stay synced=0 and retry next cycle.
        for (batch in unsynced.chunked(100)) {
            api.push(batch.asStandardCostApiModels())
                .onSuccess {
                    batch.forEach { row ->
                        // Soft-deleted rows are now confirmed on the server → hard-delete locally.
                        // Active rows just flip to synced.
                        if (!row.active) dao.deleteByUid(row.id)
                        else dao.markAsSynced(row.id)
                    }
                    pushed += batch.size
                }
                .onFailure { error ->
                    ProductLogger.w("ProductStandardCostSyncDelegate", "Batch push failed", error)
                    failed += batch.size
                    lastError = error
                }
        }
        // Rule 2 (/offline-sync): nothing pushed + failures present must surface as failure, not
        // Success(0), so CentralSyncService marks the entity FAILED and retries on reconnect.
        if (pushed == 0 && failed > 0) {
            throw lastError ?: Exception("$failed standard cost(s) failed to push — will retry on reconnect")
        }
        pushed
    }

    private suspend fun pull(): Result<Int> = runCatching {
        // Incremental: only pull rows updated since the last successful pull checkpoint.
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.PRODUCT_STANDARD_COST) ?: ""
        var page = 0
        var total = 0
        var maxServerTime = ""
        var hasNext: Boolean
        do {
            val pageResp = api.pull(
                lastSync = lastSync.ifBlank { null },
                page = page,
                size = 100,
            ).getOrThrow()
            val batch = pageResp.content
            // Permanently delete locally anything the server reports as deleted/inactive.
            batch.filter { !it.active }.forEach { dao.deleteByUid(it.uid) }
            // Upsert the rest (local unsynced edits win — they aren't overwritten until pushed).
            val toUpsert = batch.filter { it.active }.map { it.toEntity() }
            if (toUpsert.isNotEmpty()) dao.insertAll(reconcileWithLocal(toUpsert))
            val batchMaxTime = batch.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
            if (batchMaxTime > maxServerTime) maxServerTime = batchMaxTime
            total += batch.size
            hasNext = pageResp.hasNext
            page++
        } while (hasNext && total < 10000)
        // Advance the checkpoint so the next pull is incremental.
        if (maxServerTime.isNotBlank()) {
            syncStateDao.setLastSyncedAtIso(SyncEntity.PRODUCT_STANDARD_COST, maxServerTime)
        }
        total
    }

    /**
     * Drops any server row whose local copy is unsynced (synced == false) — local edits win until
     * pushed (a full sync pushes first, so a still-unsynced row means the push hasn't completed).
     * Keyed by the stable cost uid.
     */
    private suspend fun reconcileWithLocal(
        entities: List<ProductStandardCostEntity>,
    ): List<ProductStandardCostEntity> {
        if (entities.isEmpty()) return entities
        val existing = dao.getAll().associateBy { it.id }
        return entities.filter { e ->
            val local = existing[e.id]
            local == null || local.synced
        }
    }
}
