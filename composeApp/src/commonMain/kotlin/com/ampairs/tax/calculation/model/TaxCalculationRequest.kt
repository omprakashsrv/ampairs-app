package com.ampairs.tax.calculation.model

import com.ampairs.tax.domain.model.TaxCodeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tax Calculation Request
 */
@Serializable
data class TaxCalculationRequest(
    @SerialName("tax_code")
    val taxCode: String,                            // "12345678", "GROCERY"

    @SerialName("tax_code_type")
    val taxCodeType: TaxCodeType,                   // HSN_CODE, TAX_CATEGORY

    @SerialName("base_amount")
    val baseAmount: Double,                         // Unit price

    @SerialName("quantity")
    val quantity: Int = 1,

    @SerialName("source_location")
    val sourceLocation: TaxJurisdiction,            // Seller location

    @SerialName("destination_location")
    val destinationLocation: TaxJurisdiction,       // Buyer location

    @SerialName("transaction_type")
    val transactionType: TransactionType,           // B2B, B2C, EXPORT, etc.

    @SerialName("transaction_context")
    val transactionContext: TransactionContext      // Additional context
)

/**
 * Tax Jurisdiction
 */
@Serializable
data class TaxJurisdiction(
    @SerialName("country")
    val country: String,                            // ISO 3166-1 alpha-2

    @SerialName("state")
    val state: String? = null,                      // State/Province code

    @SerialName("county")
    val county: String? = null,                     // County

    @SerialName("city")
    val city: String? = null,                       // City

    @SerialName("postal_code")
    val postalCode: String? = null                  // Postal/ZIP code
)

/**
 * Transaction Type
 */
@Serializable
enum class TransactionType {
    @SerialName("B2B")
    B2B,                // Business to Business

    @SerialName("B2C")
    B2C,                // Business to Consumer

    @SerialName("EXPORT")
    EXPORT,             // Export (usually zero-rated)

    @SerialName("IMPORT")
    IMPORT,             // Import

    @SerialName("INTRA_EU")
    INTRA_EU,           // Intra-EU (for VAT)

    @SerialName("DOMESTIC")
    DOMESTIC            // Domestic transaction
}

/**
 * Transaction Context
 */
@Serializable
data class TransactionContext(
    @SerialName("business_type")
    val businessType: String = "REGULAR",           // REGULAR, COMPOSITION, SEZ

    @SerialName("is_reverse_charge")
    val isReverseCharge: Boolean = false,           // Reverse charge applicable

    @SerialName("exemption_reason")
    val exemptionReason: String? = null,            // If exempt

    @SerialName("special_conditions")
    val specialConditions: Map<String, String> = emptyMap()
)
