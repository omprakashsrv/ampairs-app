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

    suspend fun bulkUpsertGroups(groups: List<CustomerGroup>): Result<List<CustomerGroup>>

    suspend fun deleteCustomerGroup(id: String): Response<Unit>
}