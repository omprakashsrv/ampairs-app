package com.ampairs.tax.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.TaxComponentType
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.WorkspaceTaxComponent
import com.ampairs.tax.domain.model.TaxConfiguration

/**
 * Tax Configuration API Interface
 */
interface TaxConfigurationApi {

    // Workspace Configuration
    suspend fun getWorkspaceConfiguration(): Result<TaxConfiguration>
    suspend fun createWorkspaceConfiguration(config: TaxConfiguration): Result<TaxConfiguration>
    suspend fun updateWorkspaceConfiguration(config: TaxConfiguration): Result<TaxConfiguration>

    // Master Tax Codes (Server-side only)
    suspend fun searchMasterTaxCodes(
        query: String,
        countryCode: String,
        codeType: String? = null,
        category: String? = null,
        page: Int = 0,
        size: Int = 50
    ): Result<PageResponse<MasterTaxCode>>

    suspend fun getMasterTaxCode(codeId: String): Result<MasterTaxCode>

    suspend fun getPopularTaxCodes(
        countryCode: String,
        industry: String? = null,
        limit: Int = 20
    ): Result<List<MasterTaxCode>>

    // Workspace Tax Codes (Synced to mobile)
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

    suspend fun unsubscribeFromTaxCode(
        workspaceTaxCodeId: String
    ): Result<Unit>

    suspend fun bulkSubscribeTaxCodes(
        masterTaxCodeIds: List<String>,
        applyDefaultRules: Boolean = true
    ): Result<BulkSubscribeResult>

    // Tax Component Types
    suspend fun getComponentTypes(countryCode: String): Result<List<TaxComponentType>>

    // Workspace Tax Components
    suspend fun getWorkspaceComponents(
        modifiedAfter: Long? = null
    ): Result<List<WorkspaceTaxComponent>>

    // Tax Rules
    suspend fun getTaxRules(
        modifiedAfter: Long? = null,
        page: Int = 0,
        size: Int = 100
    ): Result<PageResponse<TaxRule>>

    suspend fun getTaxRulesByTaxCode(
        taxCodeId: String
    ): Result<List<TaxRule>>

    suspend fun createTaxRule(
        rule: TaxRule
    ): Result<TaxRule>

    suspend fun updateTaxRule(
        ruleId: String,
        rule: TaxRule
    ): Result<TaxRule>

    suspend fun bulkImportTaxRules(
        rules: List<TaxRule>
    ): Result<BulkImportResult>
}

/**
 * Bulk Subscribe Result
 */
data class BulkSubscribeResult(
    val successCount: Int,
    val failureCount: Int,
    val subscribedCodes: List<TaxCode>,
    val errors: List<String> = emptyList()
)

/**
 * Bulk Import Result
 */
data class BulkImportResult(
    val successCount: Int,
    val failureCount: Int,
    val importedRules: List<TaxRule>,
    val errors: List<String> = emptyList()
)
