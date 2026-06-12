package com.ampairs.common

import co.touchlab.kermit.Logger
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.model.RefreshToken
import com.ampairs.auth.api.model.Token
import com.ampairs.auth.api.model.asRefreshTokens
import com.ampairs.common.config.ConfigurationManager
import com.ampairs.common.model.Response
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val clientLog = Logger.withTag("KtorClient")

class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

private fun extractMeaningfulErrorFromDarwin(errorMessage: String): String {
    val quotedMessage = "\"([^\"]+)\"".toRegex().find(errorMessage)?.groupValues?.get(1)
    if (quotedMessage != null) return quotedMessage

    val codeMatch = "Code=([-0-9]+)".toRegex().find(errorMessage)
    if (codeMatch != null) {
        return when (codeMatch.groupValues[1].toIntOrNull()) {
            -1004 -> "Could not connect to the server."
            -1001 -> "Request timed out."
            -1009 -> "No internet connection."
            -1003 -> "Server not found."
            -1005 -> "Network connection was lost."
            -1200 -> "Secure connection failed."
            else -> "Network error (code ${codeMatch.groupValues[1]})"
        }
    }

    return errorMessage.lines().first().take(100)
}

fun httpClient(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
    withTimeout: Boolean = true,
    /**
     * Always encode fields at their default value in request bodies. Use for full-row
     * upsert contracts (order/invoice /sync): the backend deserializes non-nullable
     * primitives and rejects bodies with missing fields (FAIL_ON_NULL_FOR_PRIMITIVES).
     * Keep false for APIs with partial-update semantics, where an omitted field means
     * "don't change".
     */
    sendDefaults: Boolean = false,
) = HttpClient(engine) {

    clientLog.d { ConfigurationManager.logCurrentConfig() }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = sendDefaults
                explicitNulls = false
            }
        )
    }

    install(WebSockets)

    install(Logging) {
        logger = object : io.ktor.client.plugins.logging.Logger {
            override fun log(message: String) {
                if (message.startsWith("RESPONSE:")) {
                    val statusCode = message.substringAfter("RESPONSE: ").substringBefore(" ").toIntOrNull()
                    if (statusCode != null && statusCode >= 400) {
                        clientLog.e { "HTTP ERROR: $message" }
                        sendHttpErrorToSentry(statusCode, message)
                    }
                }
            }
        }
        level = LogLevel.BODY
        sanitizeHeader { header -> header == "Authorization" }
    }

    if (withTimeout) {
        install(HttpTimeout) {
            val timeout = 30000L
            connectTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }

    install(HttpCallValidator) {
        handleResponseExceptionWithRequest { exception, _ ->
            when (exception::class.simpleName) {
                "ConnectException" -> throw NetworkException(
                    "Unable to connect to server. Please check your network connection.", exception
                )
                "UnknownHostException" -> throw NetworkException(
                    "Server not found. Please check your network connection.", exception
                )
                "SocketTimeoutException" -> throw NetworkException(
                    "Request timed out. Please try again.", exception
                )
                "DarwinHttpRequestException" -> throw NetworkException(
                    extractMeaningfulErrorFromDarwin(exception.message ?: "Network error"), exception
                )
                else -> throw exception
            }
        }
    }

    install(Auth) {
        bearer {
            sendWithoutRequest { true }
            loadTokens {
                BearerTokens(
                    accessToken = tokenRepository.getAccessToken() ?: "",
                    refreshToken = tokenRepository.getRefreshToken() ?: ""
                )
            }
            refreshTokens {
                try {
                    val response: Response<Token> = client.post {
                        url(ApiUrlBuilder.authUrl("auth/v1/refresh-token"))
                        contentType(ContentType.Application.Json)
                        setBody(RefreshToken(oldTokens?.refreshToken ?: tokenRepository.getRefreshToken()))
                        markAsRefreshTokenRequest()
                    }.body()
                    val newTokens = response.data?.asRefreshTokens()
                    if (newTokens != null) {
                        tokenRepository.updateToken(newTokens.accessToken, newTokens.refreshToken)
                    } else {
                        tokenRepository.clearTokens()
                        UnauthenticatedHandler.onUnauthenticated()
                    }
                    newTokens
                } catch (e: Exception) {
                    clientLog.e(e) { "Token refresh failed" }
                    tokenRepository.clearTokens()
                    UnauthenticatedHandler.onUnauthenticated()
                    null
                }
            }
        }
    }

    expectSuccess = false

    defaultRequest {
        val workspaceId = tokenRepository.getWorkspaceIdSync()
        if (workspaceId.isNotEmpty()) {
            header("X-Workspace-ID", workspaceId)
        }
        contentType(ContentType.Application.Json)
    }
}

private fun sendHttpErrorToSentry(statusCode: Int, errorContext: String) {
    try {
        ErrorTracking.addBreadcrumb(message = "HTTP $statusCode Error", category = "http")
        val level = when {
            statusCode >= 500 -> SentryLevel.ERROR
            statusCode >= 400 -> SentryLevel.WARNING
            else -> SentryLevel.INFO
        }
        ErrorTracking.captureMessage(
            message = "HTTP $statusCode Error:\n$errorContext",
            level = level
        )
    } catch (e: Exception) {
        clientLog.e(e) { "Failed to send HTTP error to Sentry" }
    }
}
