package com.ampairs.customer.sync

import com.ampairs.common.EventType
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.toDomain
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.util.CustomerLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns ALL customer ↔ server traffic. The repository is local-only; this delegate is the single
 * place that talks to [CustomerApi]. CentralSyncService invokes it on PENDING_PUSH (bulk push),
 * PENDING_PULL (batched pull), and backend WebSocket events.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CUSTOMER)
class CustomerSyncDelegate(
    private val customerApi: CustomerApi,
    private val customerDao: CustomerDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CUSTOMER

    // Customer rows reference a group and a type — pull those first so names resolve locally.
    override val dependsOn: List<SyncEntity> =
        listOf(SyncEntity.CUSTOMER_GROUP, SyncEntity.CUSTOMER_TYPE)

    override suspend fun pullFromServer(): SyncResult =
        syncCustomersFromServerInBatches().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { handleCustomerEvent(EventType.valueOf(eventType), entityId) }.fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )

    // --- Push -------------------------------------------------------------------------------

    /**
     * Bulk push all locally unsynced rows through the unified /sync endpoint. Soft-deleted rows
     * (active = false) are sent IN-BAND in the same bulk body so the server can mark them DELETED;
     * there is no separate per-row DELETE call. After a successful push, soft-deleted rows are
     * hard-deleted locally and active rows are marked synced = true.
     */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = customerDao.getUnsyncedCustomers()
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
                        batch.forEach { customerDao.insertCustomer(it.copy(synced = true)) }
                        syncedCount += batch.size
                        continue
                    }
                    customerApi.bulkUpdateCustomers(sanitized)
                    // Push succeeded — reconcile each row locally.
                    for (entity in batch) {
                        if (!entity.active) {
                            customerDao.deleteCustomer(entity.id)
                        } else {
                            customerDao.insertCustomer(entity.copy(synced = true))
                        }
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    ErrorTracking.captureException(batchError, "CustomerSyncDelegate.pushPending.bulk")
                    CustomerLogger.w("CustomerSyncDelegate", "Batch push failed", batchError)
                    failedCount += batch.size
                }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount customer(s) failed to push — will retry on reconnect"))
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
    private suspend fun syncCustomersFromServerInBatches(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CUSTOMER) ?: ""
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                val pageResponse = customerApi.getCustomers(
                    lastSync,
                    currentPage,
                    batchSize,
                    "updatedAt",
                    "ASC",
                )

                val batchCustomers = pageResponse.content
                if (batchCustomers.isNotEmpty()) {
                    val entities = batchCustomers.mapNotNull { serverCustomer ->
                        val existing = customerDao.getCustomerById(serverCustomer.uid)
                        when {
                            existing != null && !existing.synced -> {
                                CustomerLogger.w("CustomerSyncDelegate", "Skipping server customer ${serverCustomer.uid} — unsynced local version wins")
                                null
                            }
                            serverCustomer.status?.equals("DELETED", ignoreCase = true) == true -> {
                                customerDao.deleteCustomer(serverCustomer.uid)
                                null
                            }
                            // Preserve the local Tally ref_id (GUID): the Customer domain/server
                            // payload doesn't carry it, so without this a pull nulls ref_id and the
                            // next reset Tally sync can't dedupe → duplicate customers.
                            else -> serverCustomer.toEntity().copy(synced = true, ref_id = existing?.ref_id)
                        }
                    }
                    if (entities.isNotEmpty()) customerDao.insertCustomers(entities)

                    val batchMaxTime = getMaxUpdatedAtFromServerCustomers(batchCustomers)
                    if (batchMaxTime > maxServerTime) maxServerTime = batchMaxTime

                    totalSynced += batchCustomers.size
                    CustomerLogger.i("CustomerSyncDelegate", "Pulled batch ${currentPage + 1}: ${batchCustomers.size} customers (page ${currentPage + 1}/${pageResponse.totalPages})")
                }

                currentPage++
            } while (pageResponse.hasNext && totalSynced < 10000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.CUSTOMER, maxServerTime)
            }

            CustomerLogger.i("CustomerSyncDelegate", "Pull completed: $totalSynced customers in $currentPage batches")
            Result.success(totalSynced)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerSyncDelegate", "Pull failed: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Backend event ----------------------------------------------------------------------

    private suspend fun handleCustomerEvent(eventType: EventType, customerId: String) {
        CustomerLogger.i("CustomerSyncDelegate", "📨 Event: $eventType for customer: $customerId")
        when (eventType) {
            EventType.CUSTOMER_CREATED,
            EventType.CUSTOMER_UPDATED -> refreshCustomerFromServer(customerId)

            EventType.CUSTOMER_DELETED -> {
                customerDao.deleteCustomer(customerId)
                CustomerLogger.i("CustomerSyncDelegate", "🗑️ Deleted customer: $customerId")
            }

            else -> { /* ignore */ }
        }
    }

    private suspend fun refreshCustomerFromServer(customerId: String) {
        try {
            val freshCustomer = customerApi.getCustomer(customerId)
            if (freshCustomer != null) {
                customerDao.insertCustomer(freshCustomer.toEntity().copy(synced = true))
                CustomerLogger.i("CustomerSyncDelegate", "✅ Refreshed customer from server: $customerId")
            } else {
                CustomerLogger.w("CustomerSyncDelegate", "Customer not found on server: $customerId")
            }
        } catch (e: Exception) {
            CustomerLogger.w("CustomerSyncDelegate", "Failed to refresh customer $customerId: ${e.message}")
        }
    }

    // --- Helpers ----------------------------------------------------------------------------

    private fun getMaxUpdatedAtFromServerCustomers(serverCustomers: List<Customer>): String =
        serverCustomers.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""

    // Returns null for records whose name can never be valid (< 2 chars from Tally garbage data).
    private fun Customer.sanitizeForServer(): Customer? =
        if ((name?.trim()?.length ?: 0) >= 2) this else null
}
