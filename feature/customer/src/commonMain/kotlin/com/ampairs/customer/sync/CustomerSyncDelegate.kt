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
     * Bulk push all locally unsynced rows. Deletions go through the single-row DELETE API (no bulk
     * delete endpoint); active upserts go through the bulk update API in batches of 100.
     */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = customerDao.getUnsyncedCustomers()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0

            // 1. Deletions (active = false) — DELETE API, then hard-delete locally.
            for (entity in unsynced.filter { !it.active }) {
                val uid = entity.id
                try {
                    customerApi.deleteCustomer(uid)
                    customerDao.deleteCustomer(uid)
                    syncedCount++
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    if (msg.contains("404") || msg.contains("405") || msg.contains("Not Found")) {
                        customerDao.deleteCustomer(uid)
                        syncedCount++
                    } else {
                        CustomerLogger.w("CustomerSyncDelegate", "Delete push failed for $uid: $msg")
                        failedCount++
                    }
                }
            }

            // 2. Active upserts — bulk update in batches of 100.
            val activeUnsynced = unsynced.filter { it.active }
            for (batch in activeUnsynced.chunked(100)) {
                try {
                    val sanitized = batch.mapNotNull { it.toDomain().sanitizeForServer() }
                    if (sanitized.isEmpty()) {
                        // Whole batch is unsyncable garbage — mark done locally so it stops retrying.
                        batch.forEach { customerDao.insertCustomer(it.copy(synced = true)) }
                        syncedCount += batch.size
                        continue
                    }
                    val serverCustomers = customerApi.bulkUpdateCustomers(sanitized)
                    val serverById = serverCustomers.associateBy { it.uid }
                    for (entity in batch) {
                        val serverCustomer = serverById[entity.id]
                        val resolved = serverCustomer?.toEntity()?.copy(synced = true)
                            ?: entity.copy(synced = true)
                        customerDao.insertCustomer(resolved)
                    }
                    syncedCount += batch.size
                } catch (batchError: Exception) {
                    ErrorTracking.captureException(batchError, "CustomerSyncDelegate.pushPending.bulk")
                    // Batch failed — per-row fallback so one bad record can't block the rest.
                    for (entity in batch) {
                        try {
                            val serverCustomer = pushCustomerToServer(entity.toDomain())
                            val resolved = serverCustomer?.toEntity()?.copy(synced = true)
                                ?: entity.copy(synced = true)
                            customerDao.insertCustomer(resolved)
                            syncedCount++
                        } catch (_: Exception) {
                            failedCount++
                        }
                    }
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
                            else -> serverCustomer.toEntity().copy(synced = true)
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

    // Try update first; fall back to create if the customer doesn't exist on the server yet.
    private suspend fun pushCustomerToServer(customer: Customer): Customer? {
        val sanitized = customer.sanitizeForServer() ?: run {
            CustomerLogger.w("CustomerSyncDelegate", "Skipping unsyncable customer ${customer.uid}: name '${customer.name}' too short")
            return null
        }
        return try {
            customerApi.updateCustomer(sanitized)
        } catch (_: Exception) {
            customerApi.createCustomer(sanitized)
        }
    }
}
