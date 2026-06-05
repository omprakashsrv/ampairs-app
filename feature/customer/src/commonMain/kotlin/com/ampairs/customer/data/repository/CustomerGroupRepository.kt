package com.ampairs.customer.data.repository

import dev.zacsweers.metro.Inject
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.api.CustomerGroupApi
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.toCustomerGroup
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.util.CustomerConstants
import com.ampairs.customer.util.CustomerLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for customer groups + the "import from master" feature.
 *
 * Writes (create/update/delete) persist to Room as unsynced and mark CUSTOMER_GROUP as
 * PENDING_PUSH; CentralSyncService then runs the bulk push via [CustomerGroupSyncDelegate].
 * The API is injected ONLY for [getAvailableCustomerGroupsForImport] (a non-sync feature) —
 * all push/pull/event traffic lives in the delegate.
 */
@OptIn(ExperimentalTime::class)
@Inject
class CustomerGroupRepository(
    private val customerGroupApi: CustomerGroupApi,
    private val customerGroupDao: CustomerGroupDao,
    private val syncStateDao: SyncStateDao,
) {

    fun observeCustomerGroups(): Flow<List<CustomerGroup>> =
        customerGroupDao.getAllCustomerGroups().map { entities -> entities.map { it.toCustomerGroup() } }

    fun searchCustomerGroups(query: String): Flow<List<CustomerGroup>> =
        if (query.isBlank()) {
            customerGroupDao.getAllCustomerGroups().map { entities -> entities.map { it.toCustomerGroup() } }
        } else {
            customerGroupDao.searchCustomerGroups(query).map { entities -> entities.map { it.toCustomerGroup() } }
        }

    /** Offline-first create: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun createCustomerGroup(customerGroup: CustomerGroup): Result<CustomerGroup> {
        return try {
            val uid = customerGroup.uid.ifBlank {
                UidGenerator.generateUid(CustomerConstants.CUSTOMER_GROUP_UID_PREFIX)
            }
            val customerGroupWithUid = customerGroup.copy(
                uid = uid,
                groupCode = customerGroup.groupCode?.takeIf { it.isNotBlank() }
                    ?: customerGroup.name.filter { it.isLetterOrDigit() || it == ' ' }
                        .trim().replace(' ', '_').uppercase().take(20)
                        .ifBlank { uid.takeLast(8).uppercase() }
            )
            customerGroupDao.insertCustomerGroup(customerGroupWithUid.toEntity().copy(synced = false))
            markPending()
            Result.success(customerGroupWithUid)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to create customer group", e)
            Result.failure(e)
        }
    }

    /** Offline-first update: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun updateCustomerGroup(customerGroup: CustomerGroup): Result<CustomerGroup> {
        return try {
            customerGroupDao.insertCustomerGroup(customerGroup.toEntity().copy(synced = false))
            markPending()
            Result.success(customerGroup)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to update customer group", e)
            Result.failure(e)
        }
    }

    /** Offline-first delete: mark inactive + unsynced locally and flag for automatic bulk push. */
    suspend fun deleteCustomerGroup(id: String): Result<Unit> {
        return try {
            val existing = customerGroupDao.getCustomerGroupById(id)
            if (existing != null) {
                customerGroupDao.insertCustomerGroup(existing.copy(active = false, synced = false))
                markPending()
            } else {
                customerGroupDao.deleteCustomerGroup(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to delete customer group", e)
            Result.failure(e)
        }
    }

    /** Import a master group into this workspace — persists locally and flags for bulk push. */
    suspend fun importCustomerGroup(customerGroup: CustomerGroup): Result<CustomerGroup> =
        createCustomerGroup(customerGroup)

    suspend fun bulkImportCustomerGroups(customerGroups: List<CustomerGroup>): Result<List<CustomerGroup>> {
        return try {
            val imported = customerGroups.mapNotNull { importCustomerGroup(it).getOrNull() }
            Result.success(imported)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to bulk import customer groups", e)
            Result.failure(e)
        }
    }

    suspend fun getCustomerGroupById(id: String): CustomerGroup? =
        customerGroupDao.getCustomerGroupById(id)?.toCustomerGroup()

    suspend fun getCustomerGroupByName(name: String): CustomerGroup? =
        customerGroupDao.getCustomerGroupByName(name)?.toCustomerGroup()

    /** Non-sync feature: fetch the master list of groups available to import (hits the API). */
    suspend fun getAvailableCustomerGroupsForImport(): Result<List<CustomerGroup>> {
        return try {
            val response = customerGroupApi.getAvailableCustomerGroupsForImport()
            if (response.data != null && response.error == null) {
                val existingGroups = customerGroupDao.getAllCustomerGroups().first().map { it.name }
                val availableGroups = response.data!!.filter { it.name !in existingGroups }
                Result.success(availableGroups)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load available customer groups"))
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to load available customer groups", e)
            Result.failure(e)
        }
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.CUSTOMER_GROUP, Clock.System.now().toEpochMilliseconds())
    }
}
