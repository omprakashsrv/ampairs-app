package com.ampairs.tax.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TaxCodeType {
    @SerialName("HSN_CODE") HSN_CODE,
    @SerialName("SAC_CODE") SAC_CODE,
    @SerialName("HS_CODE") HS_CODE,
    @SerialName("TAX_CATEGORY") TAX_CATEGORY,
    @SerialName("NAICS") NAICS,
    @SerialName("CN_CODE") CN_CODE,
    @SerialName("CUSTOM") CUSTOM
}
