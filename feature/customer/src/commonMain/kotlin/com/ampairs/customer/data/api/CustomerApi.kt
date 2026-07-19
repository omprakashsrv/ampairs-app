package com.ampairs.customer.data.api

import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerContactResponse
import com.ampairs.customer.domain.LinkContactRequest
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
    suspend fun bulkUpdateCustomers(customers: List<Customer>): List<Customer>
    suspend fun getCustomer(customerId: String): Customer?
    suspend fun getStates(lastSync: String = ""): List<State>
    suspend fun importState(stateCode: String): String
    suspend fun bulkImportStates(request: BulkImportRequest): BulkImportResponse
    suspend fun getAvailableStatesForImport(): List<MasterState>
    suspend fun deleteState(stateId: String)
    suspend fun getState(stateId: String): State?

    // -- Linked ecom accounts ("linked accounts" section on the customer detail screen) --
    suspend fun getContacts(customerId: String): List<CustomerContactResponse>
    suspend fun linkContact(customerId: String, request: LinkContactRequest): CustomerContactResponse
    suspend fun setContactActive(customerId: String, contactUid: String, active: Boolean): CustomerContactResponse
}