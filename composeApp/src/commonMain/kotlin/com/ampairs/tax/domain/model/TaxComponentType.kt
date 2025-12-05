package com.ampairs.tax.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tax Component Type - Master data for tax component definitions
 * (CGST, SGST, IGST, VAT, Sales Tax, etc.)
 *
 * Global reference data synced to mobile for all countries
 */
@Serializable
data class TaxComponentType(
    @SerialName("id")
    val id: String,                                 // "IN_CGST", "US_STATE_TAX"

    @SerialName("country_code")
    val countryCode: String,                        // ISO 3166-1 alpha-2

    @SerialName("component_code")
    val componentCode: String,                      // "CGST", "STATE_TAX", "VAT"

    @SerialName("component_name")
    val componentName: String,                      // Full name

    @SerialName("short_name")
    val shortName: String,                          // Short display name

    @SerialName("display_name")
    val displayName: String,                        // Localized display

    @SerialName("component_category")
    val componentCategory: TaxComponentCategory,    // FEDERAL, STATE, LOCAL, SURCHARGE

    @SerialName("calculation_method")
    val calculationMethod: TaxCalculationMethod,    // PERCENTAGE, FIXED, TIERED, COMPOUND

    @SerialName("is_mandatory")
    val isMandatory: Boolean = true,

    @SerialName("default_rate")
    val defaultRate: Double? = null,

    @SerialName("sort_order")
    val sortOrder: Int = 0,                         // Display order

    @SerialName("metadata")
    val metadata: Map<String, String> = emptyMap(), // JSON: Additional attributes

    @SerialName("is_active")
    val isActive: Boolean = true
)

/**
 * Tax Component Category
 */
@Serializable
enum class TaxComponentCategory {
    @SerialName("FEDERAL")
    FEDERAL,            // National-level taxes (CGST, IGST, GST, VAT)

    @SerialName("STATE")
    STATE,              // State/Province level (SGST, PST, State Tax)

    @SerialName("COUNTY")
    COUNTY,             // County/District level

    @SerialName("CITY")
    CITY,               // City/Municipal level

    @SerialName("SURCHARGE")
    SURCHARGE,          // Additional charges (CESS, Additional Cess)

    @SerialName("SPECIAL")
    SPECIAL,            // Special district taxes

    @SerialName("COMBINED")
    COMBINED            // Harmonized taxes (HST combines GST+PST)
}

/**
 * Tax Calculation Method
 */
@Serializable
enum class TaxCalculationMethod {
    @SerialName("PERCENTAGE")
    PERCENTAGE,         // Tax as % of base (most common)

    @SerialName("FIXED")
    FIXED,              // Fixed amount per unit

    @SerialName("TIERED")
    TIERED,             // Different rates for different slabs

    @SerialName("COMPOUND")
    COMPOUND            // Tax calculated on base + previous taxes (QST on GST+base)
}
