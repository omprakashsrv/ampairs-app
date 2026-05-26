package com.ampairs.auth.api.model

import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Token(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("access_token_expires_at") val accessTokenExpiresAt: Instant? = null,
    @SerialName("refresh_token_expires_at") val refreshTokenExpiresAt: Instant? = null,
)

fun Token.asRefreshTokens(): BearerTokens {
    return BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
}
