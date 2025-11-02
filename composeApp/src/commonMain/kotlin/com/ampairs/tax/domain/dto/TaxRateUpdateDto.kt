package com.ampairs.tax.domain.dto

import com.ampairs.tax.domain.TaxRate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaxRateUpdateDto(
    @SerialName("uid")
    val uid: String,

    @SerialName("rate_percentage")
    val ratePercentage: Double,

    @SerialName("fixed_amount_per_unit")
    val fixedAmountPerUnit: Double? = null,

    @SerialName("minimum_amount")
    val minimumAmount: Double? = null,

    @SerialName("maximum_amount")
    val maximumAmount: Double? = null,

    @SerialName("effective_to")
    val effectiveTo: String? = null,

    @SerialName("version_number")
    val versionNumber: Int = 1,

    @SerialName("notification_number")
    val notificationNumber: String? = null,

    @SerialName("notification_date")
    val notificationDate: String? = null,

    @SerialName("conditions")
    val conditions: Map<String, String> = emptyMap(),

    @SerialName("exemption_rules")
    val exemptionRules: Map<String, String> = emptyMap(),

    @SerialName("is_reverse_charge_applicable")
    val isReverseChargeApplicable: Boolean = false,

    @SerialName("is_composition_scheme_applicable")
    val isCompositionSchemeApplicable: Boolean = true,

    @SerialName("description")
    val description: String? = null,

    @SerialName("source_reference")
    val sourceReference: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true
) {
    companion object {
        fun from(taxRate: TaxRate): TaxRateUpdateDto {
            return TaxRateUpdateDto(
                uid = taxRate.id,
                ratePercentage = taxRate.ratePercentage,
                fixedAmountPerUnit = taxRate.cessAmountPerUnit,
                minimumAmount = null, // Not available in current TaxRate domain
                maximumAmount = null, // Not available in current TaxRate domain
                effectiveTo = taxRate.effectiveTo?.toString(),
                versionNumber = taxRate.versionNumber,
                notificationNumber = null, // Not available in current TaxRate domain
                notificationDate = null, // Not available in current TaxRate domain
                conditions = emptyMap(),
                exemptionRules = emptyMap(),
                isReverseChargeApplicable = false, // Not available in current TaxRate domain
                isCompositionSchemeApplicable = true,
                description = null, // Not available in current TaxRate domain
                sourceReference = null,
                isActive = taxRate.isActive
            )
        }
    }
}