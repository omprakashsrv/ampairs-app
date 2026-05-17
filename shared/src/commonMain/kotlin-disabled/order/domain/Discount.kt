package com.ampairs.order.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Discount(
    @SerialName("percent")
    var percent: Double,
    @SerialName("value")
    var value: Double
)