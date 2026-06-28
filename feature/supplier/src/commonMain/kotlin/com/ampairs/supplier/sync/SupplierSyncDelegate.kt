package com.ampairs.supplier.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.supplier.data.api.SupplierApi
import com.ampairs.supplier.data.db.SupplierDao
import com.ampairs.supplier.data.db.toDomain
import com.ampairs.supplier.data.db.toEntity
import com.ampairs.supplier.domain.Supplier
import com.ampairs.supplier.util.SupplierLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns ALL supplier ↔ server traffic. The repository is local-only; this delegate is the single
 * place that talks to [SupplierApi]. CentralSyncService invokes it on PENDING_PUSH (bulk push),
 * PENDING_PULL (batched pull), and backend WebSocket events.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.SUPPLIER)
class SupplierSyncDelegate(
    private val supplierApi: SupplierApi,
    private val supplierDao: SupplierDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.SUPPLIER

    override suspend fun pullFromServer(): SyncResult =
        syncSuppliersFromServerInBatches().fold(
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
     * Bulk push all locally unsynced rows through the unified /sync endpoint. Soft-deleted rows
     * (active = false) are sent IN-BAND in the same bulk body so the server can mark them DELETED;
     * there is no separate per-row DELETE call. After a successful push, soft-deleted rows are
     * hard-deleted locally and active rows are marked synced = true.
     */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = supplierDao.getUnsyncedSuppliers()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0

            for (batch in unsynced.chunked(100)) {
                try {
                    // Active rows are sanitized (drop unsyncable garbage names); soft-deleted rows
                    // are always included so the delete propagates server-side.
                    val sanitized = batch.mapNotNull { entity ->
                        if (!entity.active) entity.toDomain()
                        else entity.toDomain().sanitizeForServer()
                    }
                    if (sanitized.isEmpty()) {
                        // Whole batch is unsyncable garbage — mark done locally so it stops retrying.
                        batch.forEach { supplierDao.insertSupplier(it.copy(synced = true)) }
                        syncedCount += batch.size
                        continue
                    }
                    supplierApi.bulkUpdateSuppliers(sanitized)
                    // Push succeeded — reconcile each row locally.
                    for (entity in batch) {
                        if (!entity.active) {
                            supplierDao.deleteSupplier(entity.id)
                        } else {
                            supplierDao.insertSupplier(entity.copy(synced = true))
                        }
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    ErrorTracking.captureException(batchError, "SupplierSyncDelegate.pushPending.bulk")
                    SupplierLogger.w("SupplierSyncDelegate", "Batch push failed", batchError)
                    failedCount += batch.size
                }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount supplier(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Pull -------------------------------------------------------------------------------

    /**
     * Batched incremental pull. Reconciles each server row: local unsynced edits win; server rows
     * marked DELETED are permanently removed locally; everything else is upserted as synced.
     */
    private suspend fun syncSuppliersFromServerInBatches(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.SUPPLIER) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val pageResponse = supplierApi.getSuppliers(
                    lastSync,
                    currentPage,
                    batchSize,
                    "updatedAt",
                    "ASC",
                )

                val batchSuppliers = pageResponse.content
                if (batchSuppliers.isNotEmpty()) {
                    val entities = batchSuppliers.mapNotNull { serverSupplier ->
                        val existing = supplierDao.getSupplierById(serverSupplier.uid)
                        when {
                            existing != null && !existing.synced -> {
                                SupplierLogger.w("SupplierSyncDelegate", "Skipping server supplier ${serverSupplier.uid} — unsynced local version wins")
                                null
                            }
                            serverSupplier.status?.equals("DELETED", ignoreCase = true) == true -> {
                                supplierDao.deleteSupplier(serverSupplier.uid)
                                null
                            }
                            else -> serverSupplier.toEntity().copy(synced = true)
                        }
                    }
                    if (entities.isNotEmpty()) supplierDao.insertSuppliers(entities)

                    val batchMaxTime = getMaxUpdatedAtFromServerSuppliers(batchSuppliers)
                    if (batchMaxTime > maxServerTime) maxServerTime = batchMaxTime

                    totalSynced += batchSuppliers.size
                    SupplierLogger.i("SupplierSyncDelegate", "Pulled batch ${currentPage + 1}: ${batchSuppliers.size} suppliers (page ${currentPage + 1}/${pageResponse.totalPages})")
                }

                currentPage++
            } while (pageResponse.hasNext && totalSynced < 10000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.SUPPLIER, maxServerTime)
            }

            SupplierLogger.i("SupplierSyncDelegate", "Pull completed: $totalSynced suppliers in $currentPage batches")
            Result.success(totalSynced)
        } catch (e: Exception) {
            SupplierLogger.e("SupplierSyncDelegate", "Pull failed: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Helpers ----------------------------------------------------------------------------

    private fun getMaxUpdatedAtFromServerSuppliers(serverSuppliers: List<Supplier>): String =
        serverSuppliers.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""

    // Returns null for records whose name can never be valid (< 2 chars from garbage data).
    private fun Supplier.sanitizeForServer(): Supplier? =
        if (name.trim().length >= 2) this else null
}
