package com.ampairs.customer.data.api

import com.ampairs.customer.domain.CustomerType
import com.ampairs.common.model.Response
import com.ampairs.common.model.PageResponse

interface CustomerTypeApi {

    suspend fun getCustomerTypes(
        page: Int = 0,
        size: Int = 100,
        lastSyncTime: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: String = "ASC"
    ): Response<PageResponse<CustomerType>>

    suspend fun getAvailableCustomerTypesForImport(): Response<List<CustomerType>>

    suspend fun bulkUpsertTypes(types: List<CustomerType>): Result<List<CustomerType>>

    suspend fun deleteCustomerType(id: String): Response<Unit>
}