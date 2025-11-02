package com.ampairs.auth.domain

import com.ampairs.auth.api.model.Token
import com.ampairs.auth.db.entity.UserTokenEntity
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.datetime.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class UserToken(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: LocalDateTime? = null,
    val refreshTokenExpiresAt: LocalDateTime? = null,
)

@OptIn(ExperimentalTime::class)
fun Token.asDatabaseModel(userId: String = "legacy_user"): UserTokenEntity {
    return UserTokenEntity(
        seq_id = 0,
        id = "1",
        user_id = userId,
        refresh_token = refreshToken,
        access_token = accessToken,
        access_token_expires_at = accessTokenExpiresAt?.toInstant(TimeZone.currentSystemDefault())
            ?.toEpochMilliseconds(),
        refresh_token_expires_at = refreshTokenExpiresAt?.toInstant(TimeZone.currentSystemDefault())
            ?.toEpochMilliseconds(),
        expires_at = null // Deprecated field
    )
}

@OptIn(ExperimentalTime::class)
fun UserTokenEntity.asDomainModel(): UserToken {
    return UserToken(
        accessToken = access_token,
        refreshToken = refresh_token,
        accessTokenExpiresAt = access_token_expires_at?.let { millis ->
            Instant.fromEpochMilliseconds(millis).toLocalDateTime(
                TimeZone.currentSystemDefault()
            )
        },
        refreshTokenExpiresAt = refresh_token_expires_at?.let { millis ->
            Instant.fromEpochMilliseconds(millis).toLocalDateTime(
                TimeZone.currentSystemDefault()
            )
        }
    )
}

fun Token.asRefreshTokens(): BearerTokens {
    return BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
}