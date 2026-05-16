package com.ampairs.tax.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workspace Tax Configuration - Top-level tax settings for workspace
 */
@Serializable
data class TaxConfiguration(
    @SerialName("id")
    val id: String,

    @SerialName("country_code")
    val countryCode: String,                        // ISO 3166-1 alpha-2

    @SerialName("tax_strategy")
    val taxStrategy: TaxStrategy,                   // INDIA_GST, USA_SALES_TAX, etc.

    @SerialName("default_tax_code_system")
    val defaultTaxCodeSystem: TaxCodeType,          // HSN_CODE, TAX_CATEGORY

    @SerialName("tax_jurisdictions")
    val taxJurisdictions: List<String> = emptyList(), // ["MH", "KA", "DL"]

    @SerialName("industry")
    val industry: String? = null,                   // Industry sector for default codes

    @SerialName("auto_subscribe_new_codes")
    val autoSubscribeNewCodes: Boolean = false,     // Auto-add popular codes

    @SerialName("synced_at")
    val syncedAt: Long,

    @SerialName("metadata")
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Tax Strategy Enum
 */
@Serializable
enum class TaxStrategy {
    @SerialName("INDIA_GST")
    INDIA_GST,

    @SerialName("USA_SALES_TAX")
    USA_SALES_TAX,

    @SerialName("UK_VAT")
    UK_VAT,

    @SerialName("EU_VAT")
    EU_VAT,

    @SerialName("CANADA_GST_HST")
    CANADA_GST_HST,

    @SerialName("AUSTRALIA_GST")
    AUSTRALIA_GST,

    @SerialName("DEFAULT_TAX")
    DEFAULT_TAX
}
