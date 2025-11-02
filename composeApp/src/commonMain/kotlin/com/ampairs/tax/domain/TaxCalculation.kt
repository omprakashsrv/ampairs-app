package com.ampairs.tax.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaxCalculationRequest(
    @SerialName("hsn_code")
    val hsnCode: String,

    @SerialName("base_amount")
    val baseAmount: Double,

    @SerialName("quantity")
    val quantity: Int = 1,

    @SerialName("source_state")
    val sourceState: String,

    @SerialName("destination_state")
    val destinationState: String,

    @SerialName("business_type")
    val businessType: BusinessType = BusinessType.REGULAR,

    @SerialName("transaction_type")
    val transactionType: TransactionType = TransactionType.B2B
)

@Serializable
data class TaxCalculationResult(
    @SerialName("hsn_code")
    val hsnCode: String,

    @SerialName("base_amount")
    val baseAmount: Double,

    @SerialName("quantity")
    val quantity: Int,

    @SerialName("cgst_amount")
    val cgstAmount: Double,

    @SerialName("sgst_amount")
    val sgstAmount: Double,

    @SerialName("igst_amount")
    val igstAmount: Double,

    @SerialName("cess_amount")
    val cessAmount: Double,

    @SerialName("total_tax_amount")
    val totalTaxAmount: Double,

    @SerialName("total_amount")
    val totalAmount: Double,

    @SerialName("tax_breakdown")
    val taxBreakdown: List<TaxBreakdownItem>,

    @SerialName("transaction_type")
    val transactionType: TransactionType,

    @SerialName("is_intra_state")
    val isIntraState: Boolean
) {
    val effectiveGstRate: Double
        get() = if (isIntraState) {
            ((cgstAmount + sgstAmount) / baseAmount) * 100
        } else {
            (igstAmount / baseAmount) * 100
        }

    val effectiveCessRate: Double
        get() = if (cessAmount > 0) {
            (cessAmount / baseAmount) * 100
        } else {
            0.0
        }
}

@Serializable
data class TaxBreakdownItem(
    @SerialName("tax_type")
    val taxType: TaxType,

    @SerialName("rate_percentage")
    val ratePercentage: Double,

    @SerialName("taxable_amount")
    val taxableAmount: Double,

    @SerialName("tax_amount")
    val taxAmount: Double,

    @SerialName("description")
    val description: String
)

@Serializable
enum class TransactionType {
    @SerialName("b2b")
    B2B,

    @SerialName("b2c")
    B2C,

    @SerialName("export")
    EXPORT,

    @SerialName("import")
    IMPORT,

    @SerialName("composition")
    COMPOSITION
}

@Serializable
data class BulkTaxCalculationRequest(
    @SerialName("items")
    val items: List<TaxCalculationRequest>,

    @SerialName("default_source_state")
    val defaultSourceState: String,

    @SerialName("default_destination_state")
    val defaultDestinationState: String,

    @SerialName("default_business_type")
    val defaultBusinessType: BusinessType = BusinessType.REGULAR
)

@Serializable
data class BulkTaxCalculationResult(
    @SerialName("items")
    val items: List<TaxCalculationResult>,

    @SerialName("total_base_amount")
    val totalBaseAmount: Double,

    @SerialName("total_cgst_amount")
    val totalCgstAmount: Double,

    @SerialName("total_sgst_amount")
    val totalSgstAmount: Double,

    @SerialName("total_igst_amount")
    val totalIgstAmount: Double,

    @SerialName("total_cess_amount")
    val totalCessAmount: Double,

    @SerialName("total_tax_amount")
    val totalTaxAmount: Double,

    @SerialName("total_amount")
    val totalAmount: Double
)

// Indian state codes for GST calculation
object IndianStates {
    val stateCodes = mapOf(
        "AP" to "Andhra Pradesh",
        "AR" to "Arunachal Pradesh",
        "AS" to "Assam",
        "BR" to "Bihar",
        "CG" to "Chhattisgarh",
        "GA" to "Goa",
        "GJ" to "Gujarat",
        "HR" to "Haryana",
        "HP" to "Himachal Pradesh",
        "JK" to "Jammu and Kashmir",
        "JH" to "Jharkhand",
        "KA" to "Karnataka",
        "KL" to "Kerala",
        "MP" to "Madhya Pradesh",
        "MH" to "Maharashtra",
        "MN" to "Manipur",
        "ML" to "Meghalaya",
        "MZ" to "Mizoram",
        "NL" to "Nagaland",
        "OR" to "Odisha",
        "PB" to "Punjab",
        "RJ" to "Rajasthan",
        "SK" to "Sikkim",
        "TN" to "Tamil Nadu",
        "TG" to "Telangana",
        "TR" to "Tripura",
        "UP" to "Uttar Pradesh",
        "UT" to "Uttarakhand",
        "WB" to "West Bengal",
        "AN" to "Andaman and Nicobar Islands",
        "CH" to "Chandigarh",
        "DN" to "Dadra and Nagar Haveli",
        "DD" to "Daman and Diu",
        "DL" to "Delhi",
        "LD" to "Lakshadweep",
        "PY" to "Puducherry"
    )

    fun isValidStateCode(code: String): Boolean = stateCodes.containsKey(code)

    fun getStateName(code: String): String? = stateCodes[code]
}