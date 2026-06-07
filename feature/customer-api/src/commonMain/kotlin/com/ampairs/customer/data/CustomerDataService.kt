package com.ampairs.customer.data

import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerListItem

interface CustomerDataService {
    suspend fun getById(uid: String): Customer?

    /**
     * Lightweight customer lookup for pickers (e.g. the order/invoice editor). Blank [query]
     * returns the full list. Returns list items (id + name + contact) — call [getById] to load the
     * full customer (with GSTIN) once one is chosen.
     */
    suspend fun listCustomers(query: String = ""): List<CustomerListItem>
}
