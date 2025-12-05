package com.ampairs.tax.calculation.model

import com.ampairs.tax.domain.model.TaxCodeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tax Calculation Result
 */
@Serializable
data class TaxCalculationResult(
    @SerialName("tax_code")
    val taxCode: String,

    @SerialName("code_type")
    val codeType: TaxCodeType,

    @SerialName("base_amount")
    val baseAmount: Double,                         // Unit price * quantity

    @SerialName("quantity")
    val quantity: Int,

    @SerialName("tax_components")
    val taxComponents: List<TaxComponentResult>,    // Component-wise breakdown

    @SerialName("total_tax_amount")
    val totalTaxAmount: Double,                     // Sum of all taxes

    @SerialName("total_amount")
    val totalAmount: Double,                        // Base + total tax

    @SerialName("jurisdiction")
    val jurisdiction: TaxJurisdiction,

    @SerialName("transaction_context")
    val transactionContext: TransactionContext,

    @SerialName("country_code")
    val countryCode: String,

    @SerialName("metadata")
    val metadata: Map<String, String> = emptyMap()  // Additional info
)

/**
 * Tax Component Result - Individual tax component breakdown
 */
@Serializable
data class TaxComponentResult(
    @SerialName("component_id")
    val componentId: String,                        // Workspace component ID

    @SerialName("component_name")
    val componentName: String,                      // Display name (CGST, SGST, VAT, etc.)

    @SerialName("tax_type")
    val taxType: String,                            // Generic string for flexibility

    @SerialName("rate_percentage")
    val ratePercentage: Double,

    @SerialName("taxable_amount")
    val taxableAmount: Double,                      // Amount on which tax is calculated

    @SerialName("tax_amount")
    val taxAmount: Double,                          // Calculated tax

    @SerialName("description")
    val description: String,                        // "CGST @ 9.00%"

    @SerialName("is_compound")
    val isCompound: Boolean = false                 // Tax on tax
)
