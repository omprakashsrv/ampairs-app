package com.ampairs.common

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.config.ConfigurationManager
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

val TokenRepositoryKey = AttributeKey<TokenRepository>("TokenRepository")

/**
 * Custom exception for network-related errors
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Extracts meaningful error message from verbose iOS NSURLError.
 * iOS DarwinHttpRequestException includes extensive UserInfo dictionary that overwhelms UI.
 *
 * Example input: "Exception in http request: Error Domain=NSURLErrorDomain Code=-1004
 * \"Could not connect to the server.\" UserInfo={_kCFStreamErrorCodeKey=61...}"
 *
 * Example output: "Could not connect to the server."
 */
private fun extractMeaningfulErrorFromDarwin(errorMessage: String): String {
    // Extract quoted error message (e.g., "Could not connect to the server.")
    val quotedMessage = "\"([^\"]+)\"".toRegex().find(errorMessage)?.groupValues?.get(1)
    if (quotedMessage != null) {
        return quotedMessage
    }

    // Extract error code and map to friendly message
    val codeMatch = "Code=([-0-9]+)".toRegex().find(errorMessage)
    if (codeMatch != null) {
        val code = codeMatch.groupValues[1].toIntOrNull()
        return when (code) {
            -1004 -> "Could not connect to the server."
            -1001 -> "Request timed out."
            -1009 -> "No internet connection."
            -1003 -> "Server not found."
            -1005 -> "Network connection was lost."
            -1200 -> "Secure connection failed."
            else -> "Network error (code $code)"
        }
    }

    // Fallback: return first line of error message
    return errorMessage.lines().first().take(100)
}

fun httpClient(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
    withTimeout: Boolean = true,
) = HttpClient(engine) {

    // Log configuration for debugging
    println(ConfigurationManager.logCurrentConfig())

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            }
        )
    }

    // Install WebSocket plugin for real-time event synchronization
    // Required by Krossbow STOMP client for WebSocket connections
    install(WebSockets)

    install(Logging) {
        var currentStatusCode: Int? = null

        logger = object : Logger {
            override fun log(message: String) {
                // Debug: Print all messages temporarily to see the format
                // println("DEBUG LOG: $message")

                when {
                    // Detect error response (4xx, 5xx)
                    message.startsWith("RESPONSE:") -> {
                        // Extract status code (format: "RESPONSE: 500 Internal Server Error")
                        val statusCode =
                            message.substringAfter("RESPONSE: ").substringBefore(" ").toIntOrNull()
                        val isLoggingError = statusCode != null && statusCode >= 400
                        if (isLoggingError) {
                            println("❌ HTTP ERROR: $message")
                            currentStatusCode = statusCode
                            val currentErrorContext = StringBuilder()
                            currentErrorContext.appendLine(message)
                            sendHttpErrorToSentry(
                                currentStatusCode,
                                currentErrorContext.toString()
                            )
                        }
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


    // Global exception handling for network connectivity issues
    install(HttpCallValidator) {
        handleResponseExceptionWithRequest { exception, _ ->
            when (exception::class.simpleName) {
                "ConnectException" -> {
                    println("❌ Connection failed: ${exception.message}")
                    throw NetworkException(
                        "Unable to connect to server. Please check your network connection.",
                        exception
                    )
                }

                "UnknownHostException" -> {
                    println("❌ Host not found: ${exception.message}")
                    throw NetworkException(
                        "Server not found. Please check your network connection.",
                        exception
                    )
                }

                "SocketTimeoutException" -> {
                    println("❌ Request timed out: ${exception.message}")
                    throw NetworkException("Request timed out. Please try again.", exception)
                }

                "DarwinHttpRequestException" -> {
                    // iOS-specific error - extract meaningful message from verbose NSURLError
                    val cleanMessage =
                        extractMeaningfulErrorFromDarwin(exception.message ?: "Network error")
                    println("❌ iOS Network error: $cleanMessage")
                    throw NetworkException(cleanMessage, exception)
                }

                else -> {
                    println("❌ Network error: ${exception.message}")
                    throw exception
                }
            }
        }
    }

    // Disable expectSuccess so we can manually handle 401s
    expectSuccess = false

    // Default request configuration
    defaultRequest {
        // Add workspace header only if available and user has selected a workspace
        val workspaceId = runBlocking { tokenRepository.getWorkspaceId() }
        if (workspaceId.isNotEmpty()) {
            header("X-Workspace-ID", workspaceId)
        }

        // Add bearer token if available
        val accessToken = tokenRepository.getAccessToken()
        if (!accessToken.isNullOrEmpty()) {
            header("Authorization", "Bearer $accessToken")
        }

        // Set default content type
        contentType(ContentType.Application.Json)
    }
}.also { it.attributes.put(TokenRepositoryKey, tokenRepository) }

/**
 * Sends HTTP error details to Sentry for monitoring.
 * Captures status code and full error context including headers and body.
 *
 * @param statusCode The HTTP status code (e.g., 400, 500)
 * @param errorContext Full error details including response body and headers
 */
private fun sendHttpErrorToSentry(statusCode: Int, errorContext: String) {
    try {
        // Add breadcrumb for HTTP error
        ErrorTracking.addBreadcrumb(
            message = "HTTP $statusCode Error",
            category = "http"
        )

        // Determine severity based on status code
        val level = when {
            statusCode >= 500 -> SentryLevel.ERROR  // Server errors
            statusCode >= 400 -> SentryLevel.WARNING // Client errors
            else -> SentryLevel.INFO
        }

        // Capture detailed error message to Sentry
        ErrorTracking.captureMessage(
            message = "HTTP $statusCode Error:\n$errorContext",
            level = level
        )
    } catch (e: Exception) {
        // Fail silently - don't let Sentry reporting break the app
        println("Failed to send HTTP error to Sentry: ${e.message}")
    }
}
