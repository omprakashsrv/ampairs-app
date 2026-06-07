package com.ampairs.tax.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.TaxComponentType
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.WorkspaceTaxComponent
import com.ampairs.tax.domain.model.TaxConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface TaxConfigurationApi {

    // Workspace Configuration
    suspend fun getWorkspaceConfiguration(): Result<TaxConfiguration>
    suspend fun createWorkspaceConfiguration(config: TaxConfiguration): Result<TaxConfiguration>
    suspend fun updateWorkspaceConfiguration(config: TaxConfiguration): Result<TaxConfiguration>

    // Master Tax Codes (server-side search, not synced to mobile)
    suspend fun searchMasterTaxCodes(
        query: String,
        countryCode: String,
        codeType: String? = null,
        category: String? = null,
        page: Int = 0,
        size: Int = 50
    ): Result<PageResponse<MasterTaxCode>>

    suspend fun getMasterTaxCode(codeId: String): Result<MasterTaxCode>

    // Backend: GET /tax/v1/master-codes/popular → PageResponse<MasterTaxCodeDto>
    suspend fun getPopularTaxCodes(
        countryCode: String,
        industry: String? = null,
        limit: Int = 20
    ): Result<PageResponse<MasterTaxCode>>

    // Workspace Tax Codes
    suspend fun getTaxCodes(
        modifiedAfter: Long? = null,
        page: Int = 0,
        size: Int = 100
    ): Result<PageResponse<TaxCode>>

    suspend fun subscribeToTaxCode(
        masterTaxCodeId: String,
        customTaxRuleId: String? = null,
        isFavorite: Boolean = false,
        notes: String? = null
    ): Result<TaxCode>

    suspend fun unsubscribeFromTaxCode(workspaceTaxCodeId: String): Result<Unit>

    // Backend: PATCH /tax/v1/codes/{taxCodeId}
    suspend fun updateTaxCodeConfig(
        taxCodeId: String,
        isFavorite: Boolean? = null,
        notes: String? = null,
        customName: String? = null,
    ): Result<TaxCode>

    suspend fun bulkSubscribeTaxCodes(
        masterTaxCodeIds: List<String>,
        applyDefaultRules: Boolean = true
    ): Result<BulkSubscribeResult>

    // Tax Component Types
    suspend fun getComponentTypes(countryCode: String): Result<List<TaxComponentType>>

    // TODO: no backend endpoint yet for GET /tax/v1/components
    suspend fun getWorkspaceComponents(
        modifiedAfter: Long? = null,
        page: Int = 0,
        size: Int = 100
    ): Result<PageResponse<WorkspaceTaxComponent>>

    // Tax Rules
    suspend fun getTaxRules(
        modifiedAfter: Long? = null,
        page: Int = 0,
        size: Int = 100
    ): Result<PageResponse<TaxRule>>

    suspend fun getTaxRulesByTaxCode(taxCodeId: String): Result<List<TaxRule>>

    // Backend: PUT /tax/v1/rules/{ruleId}
    suspend fun updateTaxRule(ruleId: String, rule: TaxRule): Result<TaxRule>

    // Backend: DELETE /tax/v1/rules/{ruleId}
    suspend fun deleteTaxRule(ruleId: String): Result<Unit>

    // TODO: no backend endpoint yet for POST /tax/v1/rules
    suspend fun createTaxRule(rule: TaxRule): Result<TaxRule>

    // TODO: no backend endpoint yet for POST /tax/v1/rules/bulk-import
    suspend fun bulkImportTaxRules(rules: List<TaxRule>): Result<BulkImportResult>
}

@Serializable
data class BulkSubscribeResult(
    @SerialName("success_count") val successCount: Int,
    @SerialName("failure_count") val failureCount: Int,
    @SerialName("subscribed_codes") val subscribedCodes: List<TaxCode>,
    val errors: List<BulkOperationError> = emptyList(),
)

@Serializable
data class BulkOperationError(
    @SerialName("master_tax_code_id") val masterTaxCodeId: String,
    @SerialName("error_message") val errorMessage: String,
)

@Serializable
data class BulkImportResult(
    @SerialName("success_count") val successCount: Int,
    @SerialName("failure_count") val failureCount: Int,
    @SerialName("imported_rules") val importedRules: List<TaxRule>,
    val errors: List<String> = emptyList(),
)
