package com.ampairs.auth.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshToken(
    @SerialName("refresh_token") val refreshToken: String?,
    @SerialName("device_id") val deviceId: String? = null
)