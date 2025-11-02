package com.ampairs.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    @SerialName("code")
    val code: String = "0",
    @SerialName("message")
    val message: String = "",
)