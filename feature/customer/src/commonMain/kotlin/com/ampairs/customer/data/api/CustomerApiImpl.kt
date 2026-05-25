package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.delete
import com.ampairs.common.model.Response
import com.ampairs.common.model.PageResponse
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.MasterState
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class CustomerApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomers(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String
    ): PageResponse<Customer> {
        val params = mutableMapOf(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir
        )
        if (lastSync.isNotBlank()) {
            params["last_sync"] = lastSync
        }

        val response: Response<PageResponse<Customer>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/customers"),
            params
        )
        return response.data ?: PageResponse(
            content = emptyList(),
            pageNumber = page,
            pageSize = size,
            totalPages = 0,
            totalElements = 0L,
            hasNext = false,
            hasPrevious = false,
            first = true,
            last = true
        )
    }

    override suspend fun createCustomer(customer: Customer): Customer {
        val response: Response<Customer> = post(
            client,
            ApiUrlBuilder.customerUrl("v1/customers"),
            customer
        )
        return response.data ?: throw Exception("Failed to create customer")
    }

    override suspend fun updateCustomer(customer: Customer): Customer {
        val response: Response<Customer> = post(
            client,
            ApiUrlBuilder.customerUrl("v1/customers"),
            customer
        )
        return response.data ?: throw Exception("Failed to update customer")
    }

    override suspend fun deleteCustomer(customerId: String) {
        val response = delete<Response<Unit>>(
            client,
            ApiUrlBuilder.customerUrl("v1/customers/$customerId")
        )
        // Check for error in response and throw if delete failed
        response.error?.let { error ->
            throw Exception("Delete failed: ${error.message}")
        }
    }

    override suspend fun getCustomer(customerId: String): Customer? {
        return try {
            val response: Response<Customer> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/customers/$customerId")
            )
            response.data
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getStates(lastSync: String): List<State> {
        val params = if (lastSync.isNotBlank()) {
            mapOf("last_updated" to lastSync)
        } else {
            emptyMap()
        }
        val response: Response<List<State>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/states"),
            params
        )
        return response.data ?: emptyList()
    }

    override suspend fun importState(stateCode: String): String {
        val response: Response<String> = post(
            client,
            ApiUrlBuilder.customerUrl("v1/master-states/$stateCode/import"),
            null
        )
        return response.data ?: throw Exception("Failed to import state")
    }

    override suspend fun bulkImportStates(request: BulkImportRequest): BulkImportResponse {
        val response: Response<BulkImportResponse> = post(
            client,
            ApiUrlBuilder.customerUrl("v1/master-states/bulk-import"),
            request
        )
        return response.data ?: throw Exception("Failed to bulk import states")
    }

    override suspend fun getAvailableStatesForImport(workspaceId: String): List<MasterState> {
        val response: Response<List<MasterState>> = get(
            client,
            ApiUrlBuilder.customerUrl("v1/master-states/available-for-import"),
            mapOf("workspace_id" to workspaceId)
        )
        return response.data ?: emptyList()
    }

    override suspend fun deleteState(stateId: String) {
        delete<Response<Unit>>(
            client,
            ApiUrlBuilder.customerUrl("v1/states/$stateId")
        )
    }

    override suspend fun getState(stateId: String): State? {
        return try {
            val response: Response<State> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/states/$stateId")
            )
            response.data
        } catch (_: Exception) {
            null
        }
    }
}