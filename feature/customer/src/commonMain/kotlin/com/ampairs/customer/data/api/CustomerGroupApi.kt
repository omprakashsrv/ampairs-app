package com.ampairs.customer.data.api

import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.common.model.Response
import com.ampairs.common.model.PageResponse

interface CustomerGroupApi {

    suspend fun getCustomerGroups(
        page: Int = 0,
        size: Int = 100,
        lastSyncTime: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: String = "ASC"
    ): Response<PageResponse<CustomerGroup>>

    suspend fun getAvailableCustomerGroupsForImport(): Response<List<CustomerGroup>>

    suspend fun getCustomerGroupById(id: String): Response<CustomerGroup>

    suspend fun createCustomerGroup(customerGroup: CustomerGroup): Response<CustomerGroup>

    suspend fun updateCustomerGroup(id: String, customerGroup: CustomerGroup): Response<CustomerGroup>

    suspend fun deleteCustomerGroup(id: String): Response<Unit>

    suspend fun searchCustomerGroups(
        query: String,
        page: Int = 0,
        size: Int = 100
    ): Response<PageResponse<CustomerGroup>>
}