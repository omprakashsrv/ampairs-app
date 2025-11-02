package com.ampairs.auth.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthInitResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("session_id")
    val sessionId: String? = null,
    @SerialName("error")
    val error: ErrorDetail? = null
)

@Serializable
data class ErrorDetail(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String
)