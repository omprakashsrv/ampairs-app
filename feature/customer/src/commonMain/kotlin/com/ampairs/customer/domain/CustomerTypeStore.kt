package com.ampairs.customer.domain

import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.toCustomerType
import com.ampairs.customer.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import com.ampairs.customer.util.CustomerLogger

class CustomerTypeStore(
    private val customerTypeApi: CustomerTypeApi,
    private val customerTypeDao: CustomerTypeDao
) {
    val customerTypeStore: Store<CustomerTypeKey, List<CustomerType>> = StoreBuilder
        .from(
            fetcher = Fetcher.ofFlow { key ->
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = if (key.searchQuery.isNotBlank()) {
                            customerTypeApi.searchCustomerTypes(key.searchQuery, key.page, key.size)
                        } else {
                            customerTypeApi.getCustomerTypes(key.page, key.size)
                        }

                        if (response.data != null && response.error == null) {
                            emit(response.data!!.content)
                        } else {
                            throw Exception(response.error?.message ?: "Failed to fetch customer types")
                        }
                    } catch (e: Exception) {
                        CustomerLogger.e("CustomerTypeStore", "Error fetching customer types", e)
                        throw e
                    }
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerTypeKey ->
                    if (key.searchQuery.isNotBlank()) {
                        customerTypeDao.searchCustomerTypes(key.searchQuery).map { entities ->
                            entities.map { it.toCustomerType() }
                        }
                    } else {
                        customerTypeDao.getAllCustomerTypes().map { entities ->
                            entities.map { it.toCustomerType() }
                        }
                    }
                },
                writer = { _: CustomerTypeKey, customerTypes: List<CustomerType> ->
                    customerTypeDao.insertCustomerTypes(customerTypes.map { it.toEntity().copy(synced = true) })
                }
            )
        )
        .build()

    /**
     * Get all customer types for selection/autocomplete
     */
    fun getCustomerTypesFlow(): Flow<List<CustomerType>> {
        return customerTypeDao.getAllCustomerTypes().map { entities ->
            entities.map { it.toCustomerType() }
        }
    }

    /**
     * Get customer type by ID for form editing
     */
    suspend fun getCustomerTypeById(id: String): CustomerType? {
        return customerTypeDao.getCustomerTypeById(id)?.toCustomerType()
    }

    /**
     * Get customer type by name for form validation
     */
    suspend fun getCustomerTypeByName(name: String): CustomerType? {
        return customerTypeDao.getCustomerTypeByName(name)?.toCustomerType()
    }

    /**
     * Sync customer types with server - implemented directly
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
                    CustomerLogger.w("CustomerTypeStore", "Failed to sync customer type ${customerType.uid}", e)
                }
            }

            // SECOND: Sync from server in batches (incremental sync)
            val serverSyncResult = syncCustomerTypesFromServerInBatches()
            val serverSyncCount = serverSyncResult.getOrElse { 0 }

            CustomerLogger.i("CustomerTypeStore", "Sync completed: $syncedCount local → server, $serverSyncCount server → local")
            Result.success(syncedCount + serverSyncCount)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeStore", "Sync failed", e)
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
            CustomerLogger.e("CustomerTypeStore", "Failed to sync from server", e)
            Result.failure(e)
        }
    }
}