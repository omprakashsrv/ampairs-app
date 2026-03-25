package com.ampairs.invoice.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Address(
    @SerialName("street") var street: String = "",
    @SerialName("street2") var street2: String = "",
    @SerialName("address") var address: String = "",
    @SerialName("city") var city: String = "",
    @SerialName("state") var state: String = "",
    @SerialName("zip") var zip: String = "",
    @SerialName("country") var country: String = "",
    @SerialName("attention") var attention: String = "",
    @SerialName("phone") var phone: String = "",
)