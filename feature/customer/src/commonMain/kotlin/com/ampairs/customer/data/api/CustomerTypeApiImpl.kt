package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.common.delete
import com.ampairs.customer.domain.CustomerType
import com.ampairs.common.model.Response
import com.ampairs.common.model.PageResponse
import io.ktor.client.engine.HttpClientEngine

class CustomerTypeApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerTypeApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomerTypes(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String
    ): Response<PageResponse<CustomerType>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sortBy" to sortBy,
            "sortDirection" to sortDirection
        )

        lastSyncTime?.let { params["lastSyncTime"] = it }

        val response: Response<PageResponse<CustomerType>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/types"),
            params
        )
        return response
    }

    override suspend fun getAvailableCustomerTypesForImport(): Response<List<CustomerType>> {
        val response: Response<List<CustomerType>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/types/available")
        )
        return response
    }

    override suspend fun getCustomerTypeById(id: String): Response<CustomerType> {
        val response: Response<CustomerType> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/types/$id")
        )
        return response
    }

    override suspend fun createCustomerType(customerType: CustomerType): Response<CustomerType> {
        val response: Response<CustomerType> = post(
            client,
            ApiUrlBuilder.customerUrl("v1/types"),
            customerType
        )
        return response
    }

    override suspend fun updateCustomerType(id: String, customerType: CustomerType): Response<CustomerType> {
        val response: Response<CustomerType> = post(
            client,
            ApiUrlBuilder.customerUrl("v1/types/$id"),
            customerType
        )
        return response
    }

    override suspend fun deleteCustomerType(id: String): Response<Unit> {
        val response: Response<Unit> = delete(
            client,
            ApiUrlBuilder.customerUrl("v1/types/$id")
        )
        return response
    }

    override suspend fun searchCustomerTypes(
        query: String,
        page: Int,
        size: Int
    ): Response<PageResponse<CustomerType>> {
        val params = mapOf(
            "query" to query,
            "page" to page.toString(),
            "size" to size.toString()
        )
        val response: Response<PageResponse<CustomerType>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/types/search"),
            params
        )
        return response
    }
}