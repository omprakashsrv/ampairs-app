package com.ampairs.tax.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlin.time.ExperimentalTime::class)

@Serializable
data class TaxRate(
    @SerialName("id")
    val id: String = "",

    @SerialName("hsn_code")
    val hsnCode: String = "",

    @SerialName("tax_type")
    val taxType: TaxType = TaxType.GST,

    @SerialName("rate_percentage")
    val ratePercentage: Double = 0.0,

    @SerialName("cess_rate")
    val cessRate: Double? = null,

    @SerialName("cess_amount_per_unit")
    val cessAmountPerUnit: Double? = null,

    @SerialName("effective_from")
    val effectiveFrom: Long = 0,

    @SerialName("effective_to")
    val effectiveTo: Long? = null,

    @SerialName("geographical_zone")
    val geographicalZone: String = "PAN_INDIA",

    @SerialName("business_type")
    val businessType: BusinessType = BusinessType.REGULAR,

    @SerialName("version_number")
    val versionNumber: Int = 1,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    val createdAt: Long = 0,

    @SerialName("updated_at")
    val updatedAt: Long = 0
) {
    val totalTaxRate: Double
        get() = ratePercentage + (cessRate ?: 0.0)

    val isCurrentlyEffective: Boolean
        get() {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            return effectiveFrom <= now && (effectiveTo == null || effectiveTo > now)
        }

    val formattedRate: String
        get() = if (cessRate != null && cessRate > 0) {
            "${ratePercentage}% + ${cessRate}% Cess"
        } else {
            "${ratePercentage}%"
        }
}

@Serializable
enum class TaxType {
    @SerialName("gst")
    GST,

    @SerialName("cgst")
    CGST,

    @SerialName("sgst")
    SGST,

    @SerialName("igst")
    IGST,

    @SerialName("cess")
    CESS,

    @SerialName("vat")
    VAT,

    @SerialName("excise")
    EXCISE
}

@Serializable
enum class BusinessType {
    @SerialName("regular")
    REGULAR,

    @SerialName("composition")
    COMPOSITION,

    @SerialName("exempt")
    EXEMPT,

    @SerialName("nil_rated")
    NIL_RATED,

    @SerialName("zero_rated")
    ZERO_RATED
}

@Serializable
data class TaxConfiguration(
    @SerialName("hsn_code")
    val hsnCode: String,

    @SerialName("gst_rate")
    val gstRate: Double,

    @SerialName("cgst_rate")
    val cgstRate: Double,

    @SerialName("sgst_rate")
    val sgstRate: Double,

    @SerialName("igst_rate")
    val igstRate: Double,

    @SerialName("cess_rate")
    val cessRate: Double? = null,

    @SerialName("cess_amount_per_unit")
    val cessAmountPerUnit: Double? = null,

    @SerialName("effective_from")
    val effectiveFrom: Long,

    @SerialName("effective_to")
    val effectiveTo: Long? = null,

    @SerialName("business_type")
    val businessType: BusinessType = BusinessType.REGULAR
) {
    val isIntraState: Boolean
        get() = cgstRate > 0 && sgstRate > 0 && igstRate == 0.0

    val isInterState: Boolean
        get() = igstRate > 0 && cgstRate == 0.0 && sgstRate == 0.0

    val totalGstRate: Double
        get() = if (isIntraState) cgstRate + sgstRate else igstRate
}

data class TaxRateFilter(
    val hsnCode: String? = null,
    val taxType: TaxType? = null,
    val businessType: BusinessType? = null,
    val effectiveDate: Long? = null,
    val activeOnly: Boolean = true
)

data class TaxRateListItem(
    val id: String,
    val hsnCode: String,
    val hsnDescription: String,
    val gstRate: Double,
    val cessRate: Double?,
    val effectiveFrom: Long,
    val effectiveTo: Long?,
    val businessType: BusinessType,
    val isActive: Boolean
)