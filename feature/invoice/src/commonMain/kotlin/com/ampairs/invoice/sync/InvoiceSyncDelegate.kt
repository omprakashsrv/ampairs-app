package com.ampairs.invoice.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.invoice.api.InvoiceApi
import com.ampairs.invoice.api.model.InvoiceApiModel
import com.ampairs.invoice.api.model.toApiModel
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.invoice.db.dto.asDatabaseModel
import com.ampairs.invoice.db.dto.asItemDatabaseModel
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns ALL invoice ↔ server traffic (spec 010). The repository is local-only; this delegate is the
 * single place that talks to [InvoiceApi]. CentralSyncService invokes it on PENDING_PUSH (bulk
 * push), PENDING_PULL (batched pull), and backend WebSocket events.
 *
 * Endpoints target the `/v1/invoices/sync` contract (backend phase B1); until that lands the calls
 * fail and invoices stay local (offline-first by construction).
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.INVOICE)
class InvoiceSyncDelegate(
    private val invoiceApi: InvoiceApi,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.INVOICE

    // Invoices reference orders, customers, products and tax — push/pull those first. Must be
    // pushDependencies (not just dependsOn) so the PUSH sends the referenced order before the invoice
    // (the backend enforces fk_invoice_order_ref → customer_order). dependsOn inherits this list.
    override val pushDependencies: List<SyncEntity> =
        listOf(SyncEntity.ORDER, SyncEntity.CUSTOMER, SyncEntity.PRODUCT, SyncEntity.TAX)

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

    /** Bulk push all locally unsynced active invoices, in batches of 100. */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = invoiceDao.getUnsyncedInvoices()
            if (unsynced.isEmpty()) return Result.success(0)

            // Build full api models (invoice + items) straight from the entities (product_id and
            // tax_code are stored on the item entity — no product lookup needed).
            // Include soft-deleted (active = 0) lines so removed items push as in-band deletes.
            val apiModels = unsynced.map { e ->
                e.toApiModel(invoiceItemDao.getAllInvoiceItemsRaw(e.id))
            }

            var synced = 0
            var failed = 0
            for (batch in apiModels.chunked(100)) {
                try {
                    invoiceApi.bulkUpdateInvoices(batch)
                    batch.forEach {
                        invoiceDao.markAsSynced(it.id)
                        // The soft-deleted lines have now reached the server — drop them locally.
                        invoiceItemDao.deleteInactiveByInvoice(it.id)
                    }
                    synced += batch.size
                } catch (e: Exception) {
                    ErrorTracking.captureException(e, "InvoiceSyncDelegate.pushPending")
                    failed += batch.size
                }
            }

            // Unlike a leaf entity (where partial success is fine — failed rows retry next cycle),
            // INVOICE has its own push dependents. CentralSyncService only defers a dependent when
            // the dependency's push signals failure, so ANY unsynced batch here — not just a total
            // wipeout — must report failure, or a dependent could push in this same cycle against a
            // row that never actually reached the server.
            if (failed > 0) {
                Result.failure(Exception("$failed invoice(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(synced)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Pull -------------------------------------------------------------------------------

    /**
     * Batched incremental pull. Local unsynced edits win; server rows marked deleted/inactive are
     * removed locally; everything else is upserted as synced. Advances the ISO checkpoint.
     */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.INVOICE) ?: ""
            var total = 0
            var page = 0
            var maxTime = ""

            do {
                val pageResponse = invoiceApi.getInvoicesSync(lastSync, page, batchSize, "updatedAt", "ASC")
                val content = pageResponse.content
                if (content.isNotEmpty()) {
                    val toUpsert = mutableListOf<InvoiceApiModel>()
                    for (api in content) {
                        val existing = invoiceDao.selectById(api.id)
                        when {
                            existing != null && existing.synced == 0L -> { /* local unsynced wins */ }
                            api.softDeleted || !api.active -> invoiceDao.deleteById(api.id)
                            else -> toUpsert += api
                        }
                    }
                    if (toUpsert.isNotEmpty()) {
                        invoiceDao.updateInvoices(toUpsert.asDatabaseModel())
                        // Server-removed lines arrive as active = 0 — hard-delete them locally instead
                        // of upserting hidden rows; upsert the rest as synced.
                        val (activeItems, inactiveItems) = toUpsert.asItemDatabaseModel().partition { it.active == 1L }
                        if (activeItems.isNotEmpty()) invoiceItemDao.updateInvoiceItems(activeItems)
                        val inactiveIds = inactiveItems.map { it.id }
                        if (inactiveIds.isNotEmpty()) invoiceItemDao.deleteByIds(inactiveIds)
                    }
                    val batchMax = content
                        .mapNotNull { it.updatedAt?.takeIf { s -> s.isNotBlank() } }
                        .maxOrNull() ?: ""
                    if (batchMax > maxTime) maxTime = batchMax
                    total += content.size
                }
                page++
            } while (pageResponse.hasNext && total < 10000)

            if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.INVOICE, maxTime)
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
