package com.ampairs.customer.data

import com.ampairs.customer.domain.Customer

interface CustomerDataService {
    suspend fun getById(uid: String): Customer?
}
