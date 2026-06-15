package com.ampairs.customer.domain

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent

@Inject
class CustomerStore(
    private val repository: CustomerRepository,
    private val syncService: CentralSyncService,
) {

    fun observeCustomers() = repository.observeCustomers()

    fun searchCustomers(query: String) = repository.searchCustomers(query)

    fun filterCustomers(
        query: String,
        states: List<String>,
        types: List<String>,
        groups: List<String>,
    ) = repository.filterCustomers(query, states, types, groups)

    /** Scalable (FTS-backed, capped) search + filter — preferred path for the customer list screen. */
    fun searchAndFilter(
        query: String,
        states: List<String>,
        types: List<String>,
        groups: List<String>,
    ) = repository.searchAndFilter(query, states, types, groups)

    suspend fun getDistinctStates(): List<String> = repository.getDistinctStates()

    suspend fun getDistinctCustomerTypes(): List<String> = repository.getDistinctCustomerTypes()

    suspend fun getDistinctCustomerGroups(): List<String> = repository.getDistinctCustomerGroups()

    fun observeCustomer(customerId: String) = repository.observeCustomer(customerId)

    suspend fun createCustomer(customer: Customer): Result<Customer> = repository.createCustomer(customer)

    suspend fun updateCustomer(customer: Customer): Result<Customer> = repository.updateCustomer(customer)

    suspend fun deleteCustomer(customerId: String): Result<Unit> = repository.deleteCustomer(customerId)

    /** Manual refresh: fire a full (push + pull) sync through the central coordinator. */
    fun syncCustomers() = syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CUSTOMER))

    suspend fun getUniqueCities(): List<String> = repository.getUniqueCities()

    suspend fun getUniquePincodes(): List<String> = repository.getUniquePincodes()
}
