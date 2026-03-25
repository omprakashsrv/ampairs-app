package com.ampairs.customer.domain

import com.ampairs.customer.data.api.CustomerGroupApi
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.toCustomerGroup
import com.ampairs.customer.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import com.ampairs.customer.util.CustomerLogger

class CustomerGroupStore(
    private val customerGroupApi: CustomerGroupApi,
    private val customerGroupDao: CustomerGroupDao
) {
    val customerGroupStore: Store<CustomerGroupKey, List<CustomerGroup>> = StoreBuilder
        .from(
            fetcher = Fetcher.ofFlow { key ->
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = if (key.searchQuery.isNotBlank()) {
                            customerGroupApi.searchCustomerGroups(key.searchQuery, key.page, key.size)
                        } else {
                            customerGroupApi.getCustomerGroups(key.page, key.size)
                        }

                        if (response.data != null && response.error == null) {
                            emit(response.data!!.content)
                        } else {
                            throw Exception(response.error?.message ?: "Failed to fetch customer groups")
                        }
                    } catch (e: Exception) {
                        CustomerLogger.e("CustomerGroupStore", "Error fetching customer groups", e)
                        throw e
                    }
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerGroupKey ->
                    if (key.searchQuery.isNotBlank()) {
                        customerGroupDao.searchCustomerGroups(key.searchQuery).map { entities ->
                            entities.map { it.toCustomerGroup() }
                        }
                    } else {
                        customerGroupDao.getAllCustomerGroups().map { entities ->
                            entities.map { it.toCustomerGroup() }
                        }
                    }
                },
                writer = { _: CustomerGroupKey, customerGroups: List<CustomerGroup> ->
                    customerGroupDao.insertCustomerGroups(customerGroups.map { it.toEntity().copy(synced = true) })
                }
            )
        )
        .build()

    /**
     * Get all customer groups for selection/autocomplete
     */
    fun getCustomerGroupsFlow(): Flow<List<CustomerGroup>> {
        return customerGroupDao.getAllCustomerGroups().map { entities ->
            entities.map { it.toCustomerGroup() }
        }
    }

    /**
     * Get customer group by ID for form editing
     */
    suspend fun getCustomerGroupById(id: String): CustomerGroup? {
        return customerGroupDao.getCustomerGroupById(id)?.toCustomerGroup()
    }

    /**
     * Get customer group by name for form validation
     */
    suspend fun getCustomerGroupByName(name: String): CustomerGroup? {
        return customerGroupDao.getCustomerGroupByName(name)?.toCustomerGroup()
    }

    /**
     * Sync customer groups with server - implemented directly
     */
    suspend fun syncCustomerGroups(): Result<Int> {
        return try {
            // FIRST: Sync unsynced local customer groups to server (prevents data loss)
            val unsyncedGroups = customerGroupDao.getUnsyncedCustomerGroups()
            var syncedCount = 0

            for (entity in unsyncedGroups) {
                val customerGroup = entity.toCustomerGroup()
                try {
                    if (!entity.active) {
                        // Handle deleted customer groups
                        customerGroupApi.deleteCustomerGroup(customerGroup.uid)
                        customerGroupDao.deleteCustomerGroup(customerGroup.uid)
                        syncedCount++
                    } else {
                        // Handle created/updated customer groups
                        val response = if (entity.synced) {
                            customerGroupApi.updateCustomerGroup(customerGroup.uid, customerGroup)
                        } else {
                            customerGroupApi.createCustomerGroup(customerGroup)
                        }

                        if (response.data != null && response.error == null) {
                            customerGroupDao.insertCustomerGroup(response.data!!.toEntity().copy(synced = true))
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    CustomerLogger.w("CustomerGroupStore", "Failed to sync customer group ${customerGroup.uid}", e)
                }
            }

            // SECOND: Sync from server in batches (incremental sync)
            val serverSyncResult = syncCustomerGroupsFromServerInBatches()
            val serverSyncCount = serverSyncResult.getOrElse { 0 }

            CustomerLogger.i("CustomerGroupStore", "Sync completed: $syncedCount local → server, $serverSyncCount server → local")
            Result.success(syncedCount + serverSyncCount)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupStore", "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sync customer groups from server in batches using timestamp-based incremental sync
     */
    private suspend fun syncCustomerGroupsFromServerInBatches(batchSize: Int = 100): Result<Int> {
        return try {
            var totalSynced = 0
            var currentPage = 0

            do {
                // Fetch one page of customer groups
                val pageResponse = customerGroupApi.getCustomerGroups(currentPage, batchSize)
                val batchGroups = pageResponse.data?.content ?: emptyList()

                // Process batch with conflict resolution
                val groupsToInsert = batchGroups.mapNotNull { serverGroup ->
                    val existing = customerGroupDao.getCustomerGroupById(serverGroup.uid)
                    if (existing != null && !existing.synced) {
                        // Skip server entity to preserve local changes
                        null
                    } else {
                        serverGroup.toEntity().copy(synced = true)
                    }
                }

                if (groupsToInsert.isNotEmpty()) {
                    customerGroupDao.insertCustomerGroups(groupsToInsert)
                    totalSynced += groupsToInsert.size
                }

                currentPage++
            } while (batchGroups.size == batchSize && totalSynced < 10000) // Safety limit

            Result.success(totalSynced)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupStore", "Failed to sync from server", e)
            Result.failure(e)
        }
    }
}