package com.ampairs.order.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaxInfoApiModel(
    @SerialName("id") var id: String = "",
    @SerialName("name") var name: String = "",
    @SerialName("percentage") var percentage: Double = 0.0,
    @SerialName("formatted_name") var formattedName: String = "",
    @SerialName("tax_spec") var taxSpec: String = "",
    @SerialName("value") var value: Double? = 0.0,
)