package com.ampairs.customer.data.repository

import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.toCustomerType
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.CustomerType
import com.ampairs.customer.domain.CustomerTypeKey
import com.ampairs.customer.domain.CustomerTypeStore
import com.ampairs.customer.util.CustomerConstants
import com.ampairs.customer.util.CustomerLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

class CustomerTypeRepository(
    private val customerTypeApi: CustomerTypeApi,
    private val customerTypeDao: CustomerTypeDao,
    private val customerTypeStore: CustomerTypeStore
) {

    /**
     * Get customer types with Store5 integration for offline-first
     */
    fun getCustomerTypesFlow(
        page: Int = 0,
        size: Int = 100,
        forceRefresh: Boolean = false
    ): Flow<StoreReadResponse<List<CustomerType>>> {
        val key = CustomerTypeKey(page = page, size = size)
        return customerTypeStore.customerTypeStore.stream(
            StoreReadRequest.cached(key, refresh = forceRefresh)
        )
    }

    /**
     * Search customer types
     */
    fun searchCustomerTypes(
        query: String,
        page: Int = 0,
        size: Int = 100
    ): Flow<List<CustomerType>> {
        return if (query.isBlank()) {
            customerTypeDao.getAllCustomerTypes().map { entities ->
                entities.map { it.toCustomerType() }
            }
        } else {
            customerTypeDao.searchCustomerTypes(query).map { entities ->
                entities.map { it.toCustomerType() }
            }
        }
    }

    /**
     * Get all customer types for dropdown/autocomplete
     */
    fun getAllCustomerTypesFlow(): Flow<List<CustomerType>> {
        return customerTypeStore.getCustomerTypesFlow()
    }

    /**
     * Create a new customer type
     */
    suspend fun createCustomerType(customerType: CustomerType): Result<CustomerType> {
        return try {
            // Generate UID if not provided
            val uid = if (customerType.uid.isBlank()) {
                UidGenerator.generateUid(CustomerConstants.UID_PREFIX)
            } else {
                customerType.uid
            }

            val customerTypeWithUid = customerType.copy(uid = uid)

            // 1. Save locally first with unsynced status
            val unsyncedEntity = customerTypeWithUid.toEntity().copy(synced = false)
            customerTypeDao.insertCustomerType(unsyncedEntity)

            // 2. Try to sync with server
            try {
                val response = customerTypeApi.createCustomerType(customerTypeWithUid)
                if (response.data != null && response.error == null) {
                    // Server success - correct UID if needed and mark as synced
                    val serverCustomerType = response.data!!
                    val finalCustomerType = if (serverCustomerType.uid != uid) {
                        serverCustomerType.copy(uid = uid) // Keep local UID consistent
                    } else {
                        serverCustomerType
                    }
                    customerTypeDao.insertCustomerType(finalCustomerType.toEntity().copy(synced = true))
                    Result.success(finalCustomerType)
                } else {
                    // Server failed but data is saved locally
                    Result.success(customerTypeWithUid)
                }
            } catch (e: Exception) {
                CustomerLogger.w("CustomerTypeRepository", "Server sync failed, using local data", e)
                Result.success(customerTypeWithUid)
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to create customer type", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing customer type
     */
    suspend fun updateCustomerType(customerType: CustomerType): Result<CustomerType> {
        return try {
            // 1. Save locally first with unsynced status
            val unsyncedEntity = customerType.toEntity().copy(synced = false)
            customerTypeDao.updateCustomerType(unsyncedEntity)

            // 2. Try to sync with server
            try {
                val response = customerTypeApi.updateCustomerType(customerType.uid, customerType)
                if (response.data != null && response.error == null) {
                    customerTypeDao.insertCustomerType(response.data!!.toEntity().copy(synced = true))
                    Result.success(response.data!!)
                } else {
                    Result.success(customerType)
                }
            } catch (e: Exception) {
                CustomerLogger.w("CustomerTypeRepository", "Server update failed, using local data", e)
                Result.success(customerType)
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to update customer type", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a customer type
     */
    suspend fun deleteCustomerType(id: String): Result<Unit> {
        return try {
            // 1. Mark as inactive locally first
            customerTypeDao.deleteCustomerType(id)

            // 2. Try to delete from server
            try {
                customerTypeApi.deleteCustomerType(id)
            } catch (e: Exception) {
                CustomerLogger.w("CustomerTypeRepository", "Server delete failed, marked inactive locally", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to delete customer type", e)
            Result.failure(e)
        }
    }

    /**
     * Import customer types from available list
     */
    suspend fun importCustomerType(customerType: CustomerType): Result<CustomerType> {
        return createCustomerType(customerType)
    }

    /**
     * Bulk import customer types
     */
    suspend fun bulkImportCustomerTypes(customerTypes: List<CustomerType>): Result<List<CustomerType>> {
        return try {
            val results = customerTypes.map { customerType ->
                importCustomerType(customerType)
            }

            val successfulImports = results.mapNotNull { result ->
                result.getOrNull()
            }

            Result.success(successfulImports)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to bulk import customer types", e)
            Result.failure(e)
        }
    }

    /**
     * Get customer type by ID
     */
    suspend fun getCustomerTypeById(id: String): CustomerType? {
        return customerTypeStore.getCustomerTypeById(id)
    }

    /**
     * Get customer type by name
     */
    suspend fun getCustomerTypeByName(name: String): CustomerType? {
        return customerTypeStore.getCustomerTypeByName(name)
    }

    /**
     * Get available customer types for import
     */
    suspend fun getAvailableCustomerTypesForImport(): Result<List<CustomerType>> {
        return try {
            val response = customerTypeApi.getAvailableCustomerTypesForImport()
            if (response.data != null && response.error == null) {
                // Filter out already imported customer types
                val existingTypes = customerTypeDao.getAllCustomerTypes().first().map { it.name }
                val availableTypes = response.data!!.filter { it.name !in existingTypes }
                Result.success(availableTypes)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load available customer types"))
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to load available customer types", e)
            Result.failure(e)
        }
    }

    /**
     * Sync customer types with server using offline-first pattern
     */
    suspend fun syncCustomerTypes(): Result<Int> {
        return try {
            // FIRST: Sync unsynced local customer types to server (prevents data loss)
            val unsyncedTypes = customerTypeDao.getUnsyncedCustomerTypes()
            var syncedCount = 0

            for (entity in unsyncedTypes) {
                val customerType = entity.toCustomerType()
                try {
                    if (!entity.active) {
                        // Handle deleted customer types
                        customerTypeApi.deleteCustomerType(customerType.uid)
                        customerTypeDao.deleteCustomerType(customerType.uid)
                        syncedCount++
                    } else {
                        // Handle created/updated customer types
                        val response = if (entity.synced) {
                            customerTypeApi.updateCustomerType(customerType.uid, customerType)
                        } else {
                            customerTypeApi.createCustomerType(customerType)
                        }

                        if (response.data != null && response.error == null) {
                            customerTypeDao.insertCustomerType(response.data!!.toEntity().copy(synced = true))
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    CustomerLogger.w("CustomerTypeRepository", "Failed to sync customer type ${customerType.uid}", e)
                }
            }

            // SECOND: Sync from server in batches (incremental sync)
            val serverSyncResult = syncCustomerTypesFromServerInBatches()
            val serverSyncCount = serverSyncResult.getOrElse { 0 }

            CustomerLogger.i("CustomerTypeRepository", "Sync completed: $syncedCount local → server, $serverSyncCount server → local")
            Result.success(syncedCount + serverSyncCount)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sync customer types from server in batches using timestamp-based incremental sync
     */
    private suspend fun syncCustomerTypesFromServerInBatches(batchSize: Int = 100): Result<Int> {
        return try {
            var totalSynced = 0
            var currentPage = 0

            do {
                // Fetch one page of customer types
                val pageResponse = customerTypeApi.getCustomerTypes(currentPage, batchSize)
                val batchTypes = pageResponse.data?.content ?: emptyList()

                // Process batch with conflict resolution
                val typesToInsert = batchTypes.mapNotNull { serverType ->
                    val existing = customerTypeDao.getCustomerTypeById(serverType.uid)
                    if (existing != null && !existing.synced) {
                        // Skip server entity to preserve local changes
                        null
                    } else {
                        serverType.toEntity().copy(synced = true)
                    }
                }

                if (typesToInsert.isNotEmpty()) {
                    customerTypeDao.insertCustomerTypes(typesToInsert)
                    totalSynced += typesToInsert.size
                }

                currentPage++
            } while (batchTypes.size == batchSize && totalSynced < 10000) // Safety limit

            Result.success(totalSynced)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to sync from server", e)
            Result.failure(e)
        }
    }
}