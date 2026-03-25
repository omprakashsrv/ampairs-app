package com.ampairs.customer.data.api

import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.MasterState
import com.ampairs.common.model.PageResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class BulkImportRequest(
    @SerialName("state_codes")
    val stateCodes: List<String>
)

@Serializable
data class BulkImportResponse(
    @SerialName("imported_count")
    val imported_count: Int,
    @SerialName("imported_states")
    val imported_states: List<ImportedStateInfo>
)

@Serializable
data class ImportedStateInfo(
    val uid: String,
    val name: String,
    @SerialName("master_state_code")
    val master_state_code: String
)

interface CustomerApi {
    suspend fun getCustomers(
        lastSync: String = "",
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC"
    ): PageResponse<Customer>
    suspend fun createCustomer(customer: Customer): Customer
    suspend fun updateCustomer(customer: Customer): Customer
    suspend fun deleteCustomer(customerId: String)
    suspend fun getCustomer(customerId: String): Customer?
    suspend fun getStates(lastSync: String = ""): List<State>
    suspend fun importState(stateCode: String): String
    suspend fun bulkImportStates(request: BulkImportRequest): BulkImportResponse
    suspend fun getAvailableStatesForImport(workspaceId: String): List<MasterState>
    suspend fun deleteState(stateId: String)
    suspend fun getState(stateId: String): State?
}