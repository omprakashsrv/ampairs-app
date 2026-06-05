package com.ampairs.customer.data.repository

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.customer.data.db.toDomain
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.customer.domain.toListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.ampairs.customer.util.CustomerConstants.ERROR_CUSTOMER_UID_REQUIRED
import com.ampairs.common.cache.CacheCleanable
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.util.CustomerLogger

/**
 * Local-only data access for customers. This layer NEVER touches [CustomerApi] — the API is a
 * separate concern owned by the sync layer ([CustomerSyncDelegate], driven by CentralSyncService).
 *
 * Every write goes to Room with `synced = false` and then marks the CUSTOMER entity as
 * PENDING_PUSH via [SyncStateDao]. CentralSyncService's reactive observer picks that up and runs
 * the bulk push automatically. Pulls and pushes live in the delegate, not here.
 */
@OptIn(ExperimentalTime::class)
@Inject
class CustomerRepository(
    private val customerDao: CustomerDao,
    private val syncStateDao: SyncStateDao,
) : CustomerDataService, CacheCleanable {

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

    suspend fun getCustomerCount(): Int = customerDao.getCustomerCount()

    suspend fun getUniqueCities(): List<String> = customerDao.getUniqueCities()

    suspend fun getUniquePincodes(): List<String> = customerDao.getUniquePincodes()

    /**
     * Offline-first create: persist locally as unsynced and flag the entity for an automatic
     * bulk push. The UID must already be set by the ViewModel.
     */
    suspend fun createCustomer(customer: Customer): Result<Customer> {
        require(customer.uid.isNotBlank()) { ERROR_CUSTOMER_UID_REQUIRED }
        customerDao.insertCustomer(customer.toEntity().copy(synced = false))
        markPending()
        CustomerLogger.i("CustomerRepository", "✅ Customer saved locally (unsynced): ${customer.uid}")
        return Result.success(customer)
    }

    /** Offline-first update: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun updateCustomer(customer: Customer): Result<Customer> {
        customerDao.insertCustomer(customer.toEntity().copy(synced = false))
        markPending()
        CustomerLogger.i("CustomerRepository", "✅ Customer updated locally (unsynced): ${customer.uid}")
        return Result.success(customer)
    }

    /**
     * Offline-first delete: mark inactive + unsynced locally and flag for automatic bulk push.
     * The delegate's push will issue the server DELETE and then hard-delete the local row.
     */
    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        val customer = customerDao.getCustomerById(customerId)
        if (customer != null) {
            customerDao.insertCustomer(customer.copy(active = false, synced = false))
            markPending()
            CustomerLogger.i("CustomerRepository", "✅ Customer marked deleted locally (unsynced): $customerId")
        } else {
            CustomerLogger.w("CustomerRepository", "⚠️ Customer not found in local database: $customerId")
        }
        return Result.success(Unit)
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.CUSTOMER, Clock.System.now().toEpochMilliseconds())
    }
}
