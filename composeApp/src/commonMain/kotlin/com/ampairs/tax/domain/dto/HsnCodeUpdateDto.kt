package com.ampairs.tax.domain.dto

import com.ampairs.tax.domain.HsnCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HsnCodeUpdateDto(
    @SerialName("uid")
    val uid: String,

    @SerialName("hsn_description")
    val hsnDescription: String,

    @SerialName("unit_of_measurement")
    val unitOfMeasurement: String? = null,

    @SerialName("exemption_available")
    val exemptionAvailable: Boolean = false,

    @SerialName("business_category_rules")
    val businessCategoryRules: Map<String, String> = emptyMap(),

    @SerialName("attributes")
    val attributes: Map<String, String> = emptyMap(),

    @SerialName("effective_from")
    val effectiveFrom: String? = null,

    @SerialName("effective_to")
    val effectiveTo: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true
) {
    companion object {
        fun from(hsnCode: HsnCode): HsnCodeUpdateDto {
            return HsnCodeUpdateDto(
                uid = hsnCode.id,
                hsnDescription = hsnCode.description,
                unitOfMeasurement = null, // Not available in current HsnCode domain
                exemptionAvailable = false, // Not available in current HsnCode domain
                businessCategoryRules = emptyMap(),
                attributes = emptyMap(),
                effectiveFrom = null,
                effectiveTo = null,
                isActive = hsnCode.isActive
            )
        }
    }
}