package com.ampairs.tax.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tax Rule - Defines how multiple components combine to form complete tax
 * References workspace tax codes for offline access
 */
@Serializable
data class TaxRule(
    @SerialName("id")
    val id: String,

    @SerialName("country_code")
    val countryCode: String,

    // Tax code reference (workspace-specific)
    @SerialName("workspace_tax_code_id")
    val workspaceTaxCodeId: String,                 // FK to workspace_tax_codes

    @SerialName("tax_code")
    val taxCode: String,                            // Cached: "12345678", "998314"

    @SerialName("tax_code_type")
    val taxCodeType: TaxCodeType,                   // Cached: HSN_CODE, SAC_CODE

    @SerialName("tax_code_description")
    val taxCodeDescription: String,                 // Cached description

    // Jurisdiction
    @SerialName("jurisdiction")
    val jurisdiction: String,                       // "MH", "CA-Los Angeles"

    @SerialName("jurisdiction_type")
    val jurisdictionType: JurisdictionLevel,        // STATE, COUNTY, CITY

    // Component composition
    @SerialName("component_composition")
    val componentComposition: TaxComponentComposition, // Composition rules

    // Transaction context
    @SerialName("applicable_transaction_types")
    val applicableTransactionTypes: List<String> = emptyList(), // ["B2B", "B2C"]

    @SerialName("applicable_business_types")
    val applicableBusinessTypes: List<String> = emptyList(),    // ["REGULAR", "COMPOSITION"]

    // Effective dates
    @SerialName("effective_from")
    val effectiveFrom: Long,

    @SerialName("effective_to")
    val effectiveTo: Long? = null,

    // Version control
    @SerialName("version")
    val version: Int = 1,

    @SerialName("replaces_rule_id")
    val replacesRuleId: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    val createdAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("sync_status")
    val syncStatus: String = "SYNCED"
)

/**
 * Tax Component Composition - How components combine
 */
@Serializable
data class TaxComponentComposition(
    @SerialName("intra_state")
    val intraState: TaxScenario? = null,            // For India: CGST + SGST

    @SerialName("inter_state")
    val interState: TaxScenario? = null,            // For India: IGST

    @SerialName("standard")
    val standard: TaxScenario? = null,              // For simple tax systems

    @SerialName("b2b")
    val b2b: TaxScenario? = null,                   // For EU: B2B rules

    @SerialName("b2c")
    val b2c: TaxScenario? = null                    // For EU: B2C rules
)

/**
 * Tax Scenario - Components for a specific scenario
 */
@Serializable
data class TaxScenario(
    @SerialName("components")
    val components: List<TaxComponentConfig>,

    @SerialName("total_rate")
    val totalRate: Double
)

/**
 * Tax Component Config - Individual component in scenario
 */
@Serializable
data class TaxComponentConfig(
    @SerialName("id")
    val id: String,                                 // Workspace component ID

    @SerialName("rate")
    val rate: Double,

    @SerialName("order")
    val order: Int,

    @SerialName("is_compound")
    val isCompound: Boolean = false                 // Tax on tax
)
