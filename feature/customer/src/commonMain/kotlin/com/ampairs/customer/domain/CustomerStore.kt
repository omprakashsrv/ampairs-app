package com.ampairs.customer.domain

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.customer.data.repository.CustomerRepository

@Inject
class CustomerStore(
    private val repository: CustomerRepository,
) {

    fun observeCustomers() = repository.observeCustomers()

    fun searchCustomers(query: String) = repository.searchCustomers(query)

    fun observeCustomer(customerId: String) = repository.observeCustomer(customerId)

    suspend fun createCustomer(customer: Customer): Result<Customer> = repository.createCustomer(customer)

    suspend fun updateCustomer(customer: Customer): Result<Customer> = repository.updateCustomer(customer)

    suspend fun deleteCustomer(customerId: String): Result<Unit> = repository.deleteCustomer(customerId)

    suspend fun syncCustomers(): Result<Int> = repository.syncCustomers()

    suspend fun getUniqueCities(): List<String> = repository.getUniqueCities()

    suspend fun getUniquePincodes(): List<String> = repository.getUniquePincodes()
}
