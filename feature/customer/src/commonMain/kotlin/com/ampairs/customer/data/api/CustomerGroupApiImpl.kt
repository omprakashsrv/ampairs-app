package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.customer.domain.CustomerGroup
import io.ktor.client.engine.HttpClientEngine

class CustomerGroupApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerGroupApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomerGroups(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String
    ): Response<PageResponse<CustomerGroup>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sortBy" to sortBy,
            "sortDirection" to sortDirection
        )

        lastSyncTime?.let { params["lastSyncTime"] = it }

        val response: Response<PageResponse<CustomerGroup>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/groups"),
            params
        )
        return response
    }

    override suspend fun getAvailableCustomerGroupsForImport(): Response<List<CustomerGroup>> {
        val response: Response<List<CustomerGroup>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/groups/available")
        )
        return response
    }

    override suspend fun getCustomerGroupById(id: String): Response<CustomerGroup> {
        val response: Response<CustomerGroup> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/groups/$id")
        )
        return response
    }

    override suspend fun createCustomerGroup(customerGroup: CustomerGroup): Response<CustomerGroup> {
        val response: Response<CustomerGroup> = post(
            client,
            ApiUrlBuilder.customerUrl("v1/groups"),
            customerGroup
        )
        return response
    }

    override suspend fun updateCustomerGroup(id: String, customerGroup: CustomerGroup): Response<CustomerGroup> {
        val response: Response<CustomerGroup> = post(
            client,
            ApiUrlBuilder.customerUrl("v1/groups/$id"),
            customerGroup
        )
        return response
    }

    override suspend fun deleteCustomerGroup(id: String): Response<Unit> {
        val response: Response<Unit> = delete(
            client,
            ApiUrlBuilder.customerUrl("v1/groups/$id")
        )
        return response
    }

    override suspend fun searchCustomerGroups(
        query: String,
        page: Int,
        size: Int
    ): Response<PageResponse<CustomerGroup>> {
        val params = mapOf(
            "query" to query,
            "page" to page.toString(),
            "size" to size.toString()
        )
        val response: Response<PageResponse<CustomerGroup>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/groups/search"),
            params
        )
        return response
    }
}