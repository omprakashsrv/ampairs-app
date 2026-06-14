package com.ampairs.tax.domain.model

import com.ampairs.common.serialization.EpochMillisSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workspace Tax Component - Workspace-specific component configuration
 * Links component types to actual rates and rules for a workspace
 */
@Serializable
data class WorkspaceTaxComponent(
    @SerialName("id")
    val id: String,

    @SerialName("component_type_id")
    val componentTypeId: String,                    // FK to tax_component_types

    // Jurisdiction
    @SerialName("jurisdiction")
    val jurisdiction: String,                       // "MH", "CA", "GB-ENG"

    @SerialName("jurisdiction_level")
    val jurisdictionLevel: JurisdictionLevel,       // FEDERAL, STATE, COUNTY, CITY

    // Rate configuration
    @SerialName("rate_percentage")
    val ratePercentage: Double,

    @SerialName("rate_type")
    val rateType: RateType = RateType.FLAT,         // FLAT, TIERED, PROGRESSIVE

    @SerialName("rate_tiers")
    val rateTiers: List<RateTier>? = null,          // For tiered rates

    // Calculation rules
    @SerialName("calculation_order")
    val calculationOrder: Int = 0,

    @SerialName("is_compound_tax")
    val isCompoundTax: Boolean = false,             // Tax on tax

    @SerialName("compound_on_components")
    val compoundOnComponents: List<String>? = null, // Component IDs

    // Applicability
    @SerialName("applicable_for")
    val applicableFor: List<String> = emptyList(),  // Transaction types

    @SerialName("exemptions")
    val exemptions: List<String> = emptyList(),     // Exemption rules

    // Effective dates
    @Serializable(with = EpochMillisSerializer::class)
    @SerialName("effective_from")
    val effectiveFrom: Long,

    @Serializable(with = EpochMillisSerializer::class)
    @SerialName("effective_to")
    val effectiveTo: Long? = null,

    // Accounting
    @SerialName("gl_account_code")
    val glAccountCode: String? = null,

    @SerialName("tax_authority_code")
    val taxAuthorityCode: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @Serializable(with = EpochMillisSerializer::class)
    @SerialName("created_at")
    val createdAt: Long,

    @Serializable(with = EpochMillisSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("sync_status")
    val syncStatus: String = "SYNCED"
)

/**
 * Jurisdiction Level
 */
@Serializable
enum class JurisdictionLevel {
    @SerialName("FEDERAL")
    FEDERAL,

    @SerialName("STATE")
    STATE,

    @SerialName("COUNTY")
    COUNTY,

    @SerialName("CITY")
    CITY,

    @SerialName("SPECIAL")
    SPECIAL
}

/**
 * Rate Type
 */
@Serializable
enum class RateType {
    @SerialName("FLAT")
    FLAT,               // Single rate

    @SerialName("TIERED")
    TIERED,             // Different rates for different slabs

    @SerialName("PROGRESSIVE")
    PROGRESSIVE         // Progressive rate structure
}

/**
 * Rate Tier for tiered tax rates
 */
@Serializable
data class RateTier(
    @SerialName("min_amount")
    val minAmount: Double,

    @SerialName("max_amount")
    val maxAmount: Double? = null,                  // null = no upper limit

    @SerialName("rate")
    val rate: Double
)
