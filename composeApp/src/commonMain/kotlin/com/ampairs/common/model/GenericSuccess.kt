package com.ampairs.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenericSuccess(
    @SerialName("success")
    val success: Boolean,
    @SerialName("message")
    val message: String,
)