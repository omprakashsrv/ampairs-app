package com.ampairs.customer.data.repository

import dev.zacsweers.metro.Inject
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.toCustomerType
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.CustomerType
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
 * Local-only data access for customer types + the "import from master" feature.
 *
 * Writes (create/update/delete) persist to Room as unsynced and mark CUSTOMER_TYPE as
 * PENDING_PUSH; CentralSyncService then runs the bulk push via [CustomerTypeSyncDelegate].
 * The API is injected ONLY for [getAvailableCustomerTypesForImport] — all push/pull/event
 * traffic lives in the delegate.
 */
@OptIn(ExperimentalTime::class)
@Inject
class CustomerTypeRepository(
    private val customerTypeApi: CustomerTypeApi,
    private val customerTypeDao: CustomerTypeDao,
    private val syncStateDao: SyncStateDao,
) {

    fun observeCustomerTypes(): Flow<List<CustomerType>> =
        customerTypeDao.getAllCustomerTypes().map { entities -> entities.map { it.toCustomerType() } }

    fun searchCustomerTypes(query: String): Flow<List<CustomerType>> =
        if (query.isBlank()) {
            customerTypeDao.getAllCustomerTypes().map { entities -> entities.map { it.toCustomerType() } }
        } else {
            customerTypeDao.searchCustomerTypes(query).map { entities -> entities.map { it.toCustomerType() } }
        }

    /** Offline-first create: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun createCustomerType(customerType: CustomerType): Result<CustomerType> {
        return try {
            val uid = customerType.uid.ifBlank {
                UidGenerator.generateUid(CustomerConstants.CUSTOMER_TYPE_UID_PREFIX)
            }
            val customerTypeWithUid = customerType.copy(uid = uid)
            customerTypeDao.insertCustomerType(customerTypeWithUid.toEntity().copy(synced = false))
            markPending()
            Result.success(customerTypeWithUid)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to create customer type", e)
            Result.failure(e)
        }
    }

    /** Offline-first update: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun updateCustomerType(customerType: CustomerType): Result<CustomerType> {
        return try {
            customerTypeDao.insertCustomerType(customerType.toEntity().copy(synced = false))
            markPending()
            Result.success(customerType)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to update customer type", e)
            Result.failure(e)
        }
    }

    /** Offline-first delete: mark inactive + unsynced locally and flag for automatic bulk push. */
    suspend fun deleteCustomerType(id: String): Result<Unit> {
        return try {
            val existing = customerTypeDao.getCustomerTypeById(id)
            if (existing != null) {
                customerTypeDao.insertCustomerType(existing.copy(active = false, synced = false))
                markPending()
            } else {
                customerTypeDao.deleteCustomerType(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to delete customer type", e)
            Result.failure(e)
        }
    }

    /** Import a master type into this workspace — persists locally and flags for bulk push. */
    suspend fun importCustomerType(customerType: CustomerType): Result<CustomerType> =
        createCustomerType(customerType)

    suspend fun bulkImportCustomerTypes(customerTypes: List<CustomerType>): Result<List<CustomerType>> {
        return try {
            val imported = customerTypes.mapNotNull { importCustomerType(it).getOrNull() }
            Result.success(imported)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to bulk import customer types", e)
            Result.failure(e)
        }
    }

    suspend fun getCustomerTypeById(id: String): CustomerType? =
        customerTypeDao.getCustomerTypeById(id)?.toCustomerType()

    /** Non-sync feature: fetch the master list of types available to import (hits the API). */
    suspend fun getAvailableCustomerTypesForImport(): Result<List<CustomerType>> {
        return try {
            val response = customerTypeApi.getAvailableCustomerTypesForImport()
            if (response.data != null && response.error == null) {
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

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.CUSTOMER_TYPE, Clock.System.now().toEpochMilliseconds())
    }
}
