package com.ampairs.customer.domain

import com.ampairs.customer.data.repository.CustomerRepository

class CustomerStore(private val repository: CustomerRepository) {

    /**
     * Observe all customers - direct repository access
     */
    fun observeCustomers() = repository.observeCustomers()

    /**
     * Search customers - direct repository access
     */
    fun searchCustomers(query: String) = repository.searchCustomers(query)

    /**
     * Observe a single customer by ID - direct repository access
     */
    fun observeCustomer(customerId: String) = repository.observeCustomer(customerId)

    suspend fun createCustomer(customer: Customer): Result<Customer> {
        return repository.createCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer): Result<Customer> {
        return repository.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return repository.deleteCustomer(customerId)
    }

    suspend fun syncCustomers(): Result<Int> {
        return repository.syncCustomers()
    }

    suspend fun getUniqueCities(): List<String> {
        return repository.getUniqueCities()
    }

    suspend fun getUniquePincodes(): List<String> {
        return repository.getUniquePincodes()
    }

}