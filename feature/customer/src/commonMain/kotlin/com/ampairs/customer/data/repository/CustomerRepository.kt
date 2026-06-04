package com.ampairs.customer.data.repository

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.customer.data.db.toDomain
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.customer.domain.toListItem
import com.ampairs.common.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.customer.util.CustomerConstants.ERROR_CUSTOMER_UID_REQUIRED
import com.ampairs.common.cache.CacheCleanable
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.util.CustomerLogger

@Inject
class CustomerRepository(
    private val customerDao: CustomerDao,
    private val customerApi: CustomerApi,
    private val syncStateDao: SyncStateDao,
) : CustomerDataService, CacheCleanable {

    private suspend fun handleCustomerEvent(eventType: EventType, customerId: String) {
        CustomerLogger.i("CustomerRepository", "📨 Received event: $eventType for customer: $customerId")

        when (eventType) {
            EventType.CUSTOMER_CREATED,
            EventType.CUSTOMER_UPDATED -> {
                // Fetch fresh data from server and update local database
                refreshCustomerFromServer(customerId)
            }

            EventType.CUSTOMER_DELETED -> {
                // Delete from local database
                customerDao.deleteCustomer(customerId)
                CustomerLogger.i("CustomerRepository", "🗑️ Deleted customer: $customerId")
            }

            else -> {
                // Ignore other event types
            }
        }
    }

    /**
     * Refresh a single customer from server (called when event received from another device).
     * Updates local Room database which automatically triggers Flow updates.
     */
    private suspend fun refreshCustomerFromServer(customerId: String) {
        try {
            // Fetch latest customer data from server
            val freshCustomer = customerApi.getCustomer(customerId)

            if (freshCustomer != null) {
                // Update Room database - this automatically triggers Flow updates!
                customerDao.insertCustomer(freshCustomer.toEntity())

                CustomerLogger.i("CustomerRepository", "✅ Refreshed customer from server: $customerId")
            } else {
                CustomerLogger.w("CustomerRepository", "Customer not found on server: $customerId")
            }
        } catch (e: Exception) {
            CustomerLogger.w("CustomerRepository", "Failed to refresh customer $customerId: ${e.message}")
            // Graceful degradation - UI continues showing cached data
        }
    }

    fun observeCustomers(): Flow<List<CustomerListItem>> {
        return customerDao.getAllCustomers()
            .map { entities -> entities.map { entity -> entity.toDomain().toListItem() } }
    }

    fun observeCustomer(customerId: String): Flow<Customer?> {
        return customerDao.observeCustomerById(customerId)
            .map { it?.toDomain() }
    }

    fun searchCustomers(query: String): Flow<List<CustomerListItem>> {
        return customerDao.searchCustomers(query)
            .map { entities -> entities.map { entity -> entity.toDomain().toListItem() } }
    }

    suspend fun getCustomer(customerId: String): Customer? {
        return customerDao.getCustomerById(customerId)?.toDomain()
    }

    override suspend fun getById(uid: String): Customer? = getCustomer(uid)

    override suspend fun clearCache() { customerDao.clearWorkspaceCustomers() }

    suspend fun createCustomer(customer: Customer): Result<Customer> {
        // Customer should already have UID assigned by ViewModel
        require(customer.uid.isNotBlank()) { ERROR_CUSTOMER_UID_REQUIRED }

        // Offline-first: Save to database first with unsynced status
        val unsyncedEntity = customer.toEntity().copy(synced = false)
        customerDao.insertCustomer(unsyncedEntity)

        // Try to sync to server in background
        try {
            val serverCustomer = customerApi.createCustomer(customer)

            // Validate server response has same UID to prevent duplicates
            if (serverCustomer.uid != customer.uid) {
                // Log warning but keep local UID to avoid duplicates
                CustomerLogger.w("CustomerRepository", "Server returned different UID: ${serverCustomer.uid} vs local: ${customer.uid}")
                val correctedServerCustomer = serverCustomer.copy(uid = customer.uid)
                val syncedEntity = correctedServerCustomer.toEntity().copy(synced = true)
                customerDao.insertCustomer(syncedEntity)
                return Result.success(correctedServerCustomer)
            }

            // Update local record with server data and mark as synced
            val syncedEntity = serverCustomer.toEntity().copy(synced = true)
            customerDao.insertCustomer(syncedEntity) // Use insert with REPLACE strategy

            return Result.success(serverCustomer)
        } catch (e: Exception) {
            // If server sync fails, customer is already saved locally as unsynced
            // It will be synced later via syncCustomers()
            ErrorTracking.captureException(e, "CustomerRepository.createCustomer")
            return Result.success(customer)
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Customer> {
        // Offline-first: Update database first with unsynced status
        val unsyncedEntity = customer.toEntity().copy(synced = false)
        customerDao.insertCustomer(unsyncedEntity) // Use insert with REPLACE strategy

        // Try to sync to server in background
        try {
            val serverCustomer = customerApi.updateCustomer(customer)

            // Update local record with server data and mark as synced
            val syncedEntity = serverCustomer.toEntity().copy(synced = true)
            customerDao.insertCustomer(syncedEntity) // Use insert with REPLACE strategy

            return Result.success(serverCustomer)
        } catch (e: Exception) {
            // If server sync fails, customer is already updated locally as unsynced
            // It will be synced later via syncCustomers()
            ErrorTracking.captureException(e, "CustomerRepository.updateCustomer")
            return Result.success(customer)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        // Offline-first: Mark as deleted locally first (BEFORE any network call)
        val customer = customerDao.getCustomerById(customerId)
        if (customer != null) {
            val deletedEntity = customer.copy(active = false, synced = false)
            customerDao.insertCustomer(deletedEntity) // Use insert with REPLACE strategy
            CustomerLogger.i("CustomerRepository", "✅ Customer marked as deleted locally: $customerId (synced=false)")
        } else {
            CustomerLogger.w("CustomerRepository", "⚠️ Customer not found in local database: $customerId")
        }

        // Try to delete on server in background (non-blocking for UI)
        try {
            customerApi.deleteCustomer(customerId)
            // If server delete succeeds, remove from local database completely
            customerDao.deleteCustomer(customerId)
            CustomerLogger.i("CustomerRepository", "✅ Customer deleted from server and removed locally: $customerId")
        } catch (e: Exception) {
            // If server delete fails (405, network error, etc.), customer remains marked as deleted locally
            // It will be synced later via syncCustomers()
            CustomerLogger.w("CustomerRepository", "⚠️ Server delete failed for $customerId: ${e.message}. Will retry during sync.")
            ErrorTracking.captureException(e, "CustomerRepository.deleteCustomer")
        }

        // Always return success since local deletion is complete
        return Result.success(Unit)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun syncCustomers(): Result<Int> {
        return try {
            // FIRST: Sync unsynced local customers to server (prevents data loss)
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            var syncedCount = 0

            // 1a. Handle deletions individually (no bulk delete API)
            for (entity in unsyncedCustomers.filter { !it.active }) {
                val customer = entity.toDomain()
                try {
                    customerApi.deleteCustomer(customer.uid)
                    customerDao.deleteCustomer(customer.uid)
                    syncedCount++
                } catch (deleteError: Exception) {
                    val msg = deleteError.message ?: ""
                    if (msg.contains("404") || msg.contains("405") || msg.contains("Not Found")) {
                        customerDao.deleteCustomer(customer.uid)
                        syncedCount++
                    } else {
                        CustomerLogger.w("CustomerRepository", "⚠️ Delete sync failed for ${customer.uid}: $msg")
                    }
                }
            }

            // 1b. Push active upserts in batches of 10 via bulk API
            val activeUnsynced = unsyncedCustomers.filter { it.active }
            for (batch in activeUnsynced.chunked(10)) {
                try {
                    val sanitized = batch.mapNotNull { it.toDomain().sanitizeForServer() }
                    if (sanitized.isEmpty()) {
                        // All records in this batch are unsyncable — mark them done locally
                        batch.forEach { customerDao.insertCustomer(it.copy(synced = true)) }
                        syncedCount += batch.size
                        continue
                    }
                    val serverCustomers = customerApi.bulkUpdateCustomers(sanitized)
                    val serverById = serverCustomers.associateBy { it.uid }
                    for (entity in batch) {
                        val serverCustomer = serverById[entity.id]
                        val resultEntity = serverCustomer?.toEntity()?.copy(synced = true)
                            ?: entity.copy(synced = true)
                        customerDao.insertCustomer(resultEntity)
                    }
                    syncedCount += batch.size
                } catch (syncError: Exception) {
                    ErrorTracking.captureException(syncError, "CustomerRepository.syncCustomers.bulk")
                    // Batch failed — individual fallback
                    for (entity in batch) {
                        try {
                            val customer = entity.toDomain()
                            val serverCustomer = pushCustomerToServer(customer)
                            val resultEntity = serverCustomer?.toEntity()?.copy(synced = true)
                                ?: entity.copy(synced = true)
                            customerDao.insertCustomer(resultEntity)
                            syncedCount++
                        } catch (_: Exception) { }
                    }
                }
            }

            // SECOND: Pull updates from server in batches (after local changes are synced)
            val batchSyncResult = syncCustomersFromServerInBatches()
            val serverSyncedCount = batchSyncResult.getOrElse { 0 }

            Result.success(syncedCount + serverSyncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerCount(): Int {
        return customerDao.getCustomerCount()
    }

    suspend fun getUniqueCities(): List<String> {
        return customerDao.getUniqueCities()
    }

    suspend fun getUniquePincodes(): List<String> {
        return customerDao.getUniquePincodes()
    }

    private suspend fun getLastSyncTime(): String {
        return syncStateDao.getLastSyncedAtIso(SyncEntity.CUSTOMER) ?: ""
    }

    /**
     * Extract the maximum updatedAt timestamp from server customers.
     * Uses string comparison since ISO 8601 timestamps are naturally sortable.
     */
    private fun getMaxUpdatedAtFromServerCustomers(serverCustomers: List<Customer>): String {
        return serverCustomers.mapNotNull { customer ->
            customer.updatedAt?.takeIf { it.isNotBlank() }
        }.maxOrNull() ?: ""
    }

    /**
     * Sync customers from server in batches to handle large datasets (10K+ customers).
     * Returns the total number of customers synced.
     */
    // --- SyncDelegate support ---

    /** Reactive count of locally unsynced rows — drives PendingPush status in CentralSyncService. */
    fun observeUnsyncedCount(): Flow<Int> = customerDao.observeUnsyncedCount()

    /** Push-only: sync all locally unsynced records to the server. */
    suspend fun pushPendingToServer(): Result<Int> {
        return try {
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            var syncedCount = 0
            var failedCount = 0
            for (entity in unsyncedCustomers) {
                val customer = entity.toDomain()
                try {
                    if (!entity.active) {
                        try {
                            customerApi.deleteCustomer(customer.uid)
                            customerDao.deleteCustomer(customer.uid)
                            syncedCount++
                        } catch (deleteError: Exception) {
                            val errorMessage = deleteError.message ?: ""
                            if (errorMessage.contains("404") || errorMessage.contains("405") || errorMessage.contains("Not Found")) {
                                customerDao.deleteCustomer(customer.uid)
                                syncedCount++
                            } else {
                                CustomerLogger.w("CustomerRepository", "Delete sync failed for ${customer.uid}: ${deleteError.message}")
                                failedCount++
                            }
                        }
                    } else {
                        val serverCustomer = pushCustomerToServer(customer)
                        if (serverCustomer != null) {
                            customerDao.insertCustomer(serverCustomer.toEntity().copy(synced = true))
                            syncedCount++
                        } else {
                            failedCount++
                        }
                    }
                } catch (e: Exception) {
                    ErrorTracking.captureException(e, "CustomerRepository.pushPendingToServer")
                    failedCount++
                }
            }
            if (failedCount > 0) Result.failure(Exception("$failedCount customer(s) failed to sync"))
            else Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Pull-only: fetch latest data from server and merge into Room. */
    suspend fun pullFromServer(): Result<Int> = syncCustomersFromServerInBatches()

    /** Handle a backend WebSocket event for a specific customer. */
    suspend fun handleExternalEvent(customerId: String, eventType: String) {
        handleCustomerEvent(
            eventType = EventType.valueOf(eventType),
            customerId = customerId,
        )
    }

    private suspend fun syncCustomersFromServerInBatches(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = getLastSyncTime()
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                // Fetch one page of customers
                val pageResponse = customerApi.getCustomers(
                    lastSync,
                    currentPage,
                    batchSize,
                    "updatedAt",
                    "ASC"
                )

                // Process this batch
                val batchCustomers = pageResponse.content
                if (batchCustomers.isNotEmpty()) {
                    // Reconcile each server row: skip if a local unsynced edit exists (local wins);
                    // permanently delete locally if the server marks it DELETED; otherwise upsert.
                    val entities = batchCustomers.mapNotNull { serverCustomer ->
                        val existingCustomer = customerDao.getCustomerById(serverCustomer.uid)
                        when {
                            existingCustomer != null && !existingCustomer.synced -> {
                                CustomerLogger.w("CustomerRepository", "Skipping server customer ${serverCustomer.uid} - unsynced local version wins")
                                null
                            }
                            serverCustomer.status?.equals("DELETED", ignoreCase = true) == true -> {
                                // Server-removed → permanently delete from local DB.
                                customerDao.deleteCustomer(serverCustomer.uid)
                                null
                            }
                            else -> serverCustomer.toEntity().copy(synced = true)
                        }
                    }
                    if (entities.isNotEmpty()) customerDao.insertCustomers(entities)

                    // Track the latest timestamp from this batch (deleted rows advance it too)
                    val batchMaxTime = getMaxUpdatedAtFromServerCustomers(batchCustomers)
                    if (batchMaxTime > maxServerTime) {
                        maxServerTime = batchMaxTime
                    }

                    totalSynced += batchCustomers.size
                    CustomerLogger.i("CustomerRepository", "Synced batch ${currentPage + 1}: ${batchCustomers.size} customers (page ${currentPage + 1}/${pageResponse.totalPages})")
                }

                currentPage++
            } while (pageResponse.hasNext && totalSynced < 10000) // Safety limit to prevent infinite loops

            // Update last sync time using the latest timestamp from all batches
            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.CUSTOMER, maxServerTime)
            }

            CustomerLogger.i("CustomerRepository", "Batch sync completed: $totalSynced customers synced in $currentPage batches")
            Result.success(totalSynced)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerRepository", "Batch sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Returns null for records whose name can never be valid (< 2 chars from Tally garbage data).
    // Phone/pincode are already cleaned by TallySyncService after each Tally sync.
    private fun Customer.sanitizeForServer(): Customer? =
        if ((name?.trim()?.length ?: 0) >= 2) this else null

    // Try update first; fall back to create if the customer doesn't exist on the server yet.
    // Returns null when the record has unrecoverable data (caller should mark synced=true locally).
    private suspend fun pushCustomerToServer(customer: Customer): Customer? {
        val sanitized = customer.sanitizeForServer() ?: run {
            CustomerLogger.w("CustomerRepository", "Skipping unsyncable customer ${customer.uid}: name '${customer.name}' is too short")
            return null
        }
        return try {
            customerApi.updateCustomer(sanitized)
        } catch (_: Exception) {
            customerApi.createCustomer(sanitized)
        }
    }

}