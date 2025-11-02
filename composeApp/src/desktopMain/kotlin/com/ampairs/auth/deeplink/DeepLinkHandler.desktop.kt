package com.ampairs.auth.deeplink

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.awt.Desktop
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Desktop deep link handler for processing authentication callbacks
 *
 * Handles URLs in the format: ampairs://auth?access_token=xxx&refresh_token=yyy
 *
 * Usage:
 * 1. Call setupDeepLinkHandler() during app initialization
 * 2. Observe deepLinkEvents flow for incoming deep links
 * 3. Process authentication tokens when received
 */
object DeepLinkHandler {

    private val _deepLinkEvents = MutableSharedFlow<DeepLinkEvent>(replay = 0)
    val deepLinkEvents: SharedFlow<DeepLinkEvent> = _deepLinkEvents.asSharedFlow()

    private var isSetup = false

    /**
     * Setup deep link handler for the desktop application
     * This should be called once during app initialization
     */
    fun setupDeepLinkHandler() {
        if (isSetup) {
            println("DeepLinkHandler: Already set up")
            return
        }

        try {
            // Check if Desktop API is supported
            if (!Desktop.isDesktopSupported()) {
                println("DeepLinkHandler: Desktop API not supported on this platform")
                return
            }

            val desktop = Desktop.getDesktop()

            // Check if URI handler is supported
            if (!desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
                println("DeepLinkHandler: URI handler not supported on this platform")
                // Continue anyway - we'll use manual deep link processing
            }

            // Set up URI handler if supported
            if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
                desktop.setOpenURIHandler { event ->
                    val uri = event.uri
                    println("DeepLinkHandler: Received deep link: $uri")
                    handleDeepLink(uri)
                }
                println("DeepLinkHandler: Successfully registered URI handler for ampairs:// scheme")
            }

            isSetup = true

        } catch (e: Exception) {
            println("DeepLinkHandler: Error setting up deep link handler: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Manually process a deep link URL string
     * This is a fallback for platforms where automatic URI handler isn't available
     */
    fun processDeepLink(urlString: String) {
        try {
            val uri = URI(urlString)
            handleDeepLink(uri)
        } catch (e: Exception) {
            println("DeepLinkHandler: Error processing deep link: ${e.message}")
            kotlinx.coroutines.runBlocking {
                _deepLinkEvents.emit(DeepLinkEvent.Error("Invalid deep link URL: ${e.message}"))
            }
        }
    }

    /**
     * Process manually pasted JSON tokens from browser
     * Expected JSON format: {"access_token": "xxx", "refresh_token": "yyy"}
     *
     * This is a fallback method for browsers that don't support deep linking.
     * Users can copy the JSON from the browser and paste it here.
     *
     * @param jsonString The JSON string containing tokens
     * @return Result with success/error message
     */
    suspend fun processManualTokens(jsonString: String): Result<Pair<String, String>> {
        return try {
            println("DeepLinkHandler: Processing manual tokens")

            // Basic JSON parsing (simple approach without external library)
            val trimmed = jsonString.trim()

            // Validate JSON structure
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return Result.failure(Exception("Invalid JSON format. Expected format: {\"access_token\": \"xxx\", \"refresh_token\": \"yyy\"}"))
            }

            // Extract tokens using regex
            val accessTokenRegex = """"access_token"\s*:\s*"([^"]+)"""".toRegex()
            val refreshTokenRegex = """"refresh_token"\s*:\s*"([^"]+)"""".toRegex()

            val accessTokenMatch = accessTokenRegex.find(trimmed)
            val refreshTokenMatch = refreshTokenRegex.find(trimmed)

            val accessToken = accessTokenMatch?.groupValues?.get(1)
            val refreshToken = refreshTokenMatch?.groupValues?.get(1)

            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                println("DeepLinkHandler: Missing tokens in JSON")
                println("DeepLinkHandler: Access token present: ${!accessToken.isNullOrBlank()}")
                println("DeepLinkHandler: Refresh token present: ${!refreshToken.isNullOrBlank()}")
                return Result.failure(Exception("Missing access_token or refresh_token in JSON"))
            }

            // Validate token format (should be non-empty and reasonable length)
            if (accessToken.length < 10 || refreshToken.length < 10) {
                return Result.failure(Exception("Invalid token format. Tokens seem too short."))
            }

            println("DeepLinkHandler: Successfully parsed manual tokens")
            println("DeepLinkHandler: Access token length: ${accessToken.length}")
            println("DeepLinkHandler: Refresh token length: ${refreshToken.length}")

            // Emit authentication event (no longer using runBlocking - this is a suspend function)
            _deepLinkEvents.emit(
                DeepLinkEvent.AuthCallback(
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )
            )

            println("DeepLinkHandler: Successfully emitted auth callback event")

            Result.success(Pair(accessToken, refreshToken))

        } catch (e: Exception) {
            println("DeepLinkHandler: Error processing manual tokens: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to process tokens: ${e.message}"))
        }
    }

    /**
     * Internal handler for processing deep link URIs
     */
    private fun handleDeepLink(uri: URI) {
        try {
            // Validate scheme
            if (uri.scheme != "ampairs") {
                println("DeepLinkHandler: Invalid scheme: ${uri.scheme}")
                return
            }

            // Route based on host
            when (uri.host) {
                "auth" -> handleAuthCallback(uri)
                else -> {
                    println("DeepLinkHandler: Unknown deep link host: ${uri.host}")
                    kotlinx.coroutines.runBlocking {
                        _deepLinkEvents.emit(DeepLinkEvent.Unknown(uri.toString()))
                    }
                }
            }

        } catch (e: Exception) {
            println("DeepLinkHandler: Error handling deep link: ${e.message}")
            e.printStackTrace()
            kotlinx.coroutines.runBlocking {
                _deepLinkEvents.emit(DeepLinkEvent.Error("Error handling deep link: ${e.message}"))
            }
        }
    }

    /**
     * Handle authentication callback from web browser
     * Expected format: ampairs://auth?access_token=xxx&refresh_token=yyy
     */
    private fun handleAuthCallback(uri: URI) {
        try {
            val params = parseQueryParams(uri.query ?: "")

            val accessToken = params["access_token"]
            val refreshToken = params["refresh_token"]

            println("DeepLinkHandler: Auth callback received")
            println("DeepLinkHandler: Access token present: ${!accessToken.isNullOrBlank()}")
            println("DeepLinkHandler: Refresh token present: ${!refreshToken.isNullOrBlank()}")

            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                println("DeepLinkHandler: Missing required tokens in auth callback")
                kotlinx.coroutines.runBlocking {
                    _deepLinkEvents.emit(
                        DeepLinkEvent.Error("Missing access_token or refresh_token in deep link")
                    )
                }
                return
            }

            // Emit authentication event
            kotlinx.coroutines.runBlocking {
                _deepLinkEvents.emit(
                    DeepLinkEvent.AuthCallback(
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    )
                )
            }

            println("DeepLinkHandler: Auth callback successfully processed")

        } catch (e: Exception) {
            println("DeepLinkHandler: Error processing auth callback: ${e.message}")
            e.printStackTrace()
            kotlinx.coroutines.runBlocking {
                _deepLinkEvents.emit(DeepLinkEvent.Error("Error processing auth callback: ${e.message}"))
            }
        }
    }

    /**
     * Parse query parameters from URI query string
     */
    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()

        return query.split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.toString())
                    val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.toString())
                    key to value
                } else {
                    null
                }
            }
            .toMap()
    }

    /**
     * Open browser to authentication URL
     * URL is determined by current environment configuration:
     * - DEV: http://localhost:4200/login?client=desktop
     * - PRODUCTION: https://app.ampairs.com/login?client=desktop
     *
     * Query parameters added:
     * - client=desktop: Indicates this is a desktop client authentication
     *
     * Can be overridden via:
     * - System property: -Dampairs.web.authUrl=<url>
     * - Environment variable: AMPAIRS_WEB_AUTH_URL=<url>
     * - Parameter: openAuthenticationBrowser("custom-url")
     */
    fun openAuthenticationBrowser(authUrl: String? = null) {
        try {
            // Use provided URL, or system property/env var, or default based on environment
            val envProperty = System.getProperty("ampairs.environment")
                ?: System.getenv("AMPAIRS_ENVIRONMENT")
                ?: "dev"

            val defaultBaseUrl = when (envProperty.lowercase()) {
                "production", "prod", "release" -> "https://app.ampairs.com/login"
                else -> "http://localhost:4200/login"
            }

            val baseUrl = authUrl
                ?: System.getProperty("ampairs.web.authUrl")
                ?: System.getenv("AMPAIRS_WEB_AUTH_URL")
                ?: defaultBaseUrl

            // Add query params to indicate desktop client
            val finalUrl = if (baseUrl.contains("?")) {
                "$baseUrl&client=desktop"
            } else {
                "$baseUrl?client=desktop"
            }

            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI(finalUrl))
                    println("DeepLinkHandler: Opened browser to: $finalUrl")
                    println("DeepLinkHandler: Environment: $envProperty")
                } else {
                    println("DeepLinkHandler: Browser action not supported")
                }
            } else {
                println("DeepLinkHandler: Desktop API not supported")
            }
        } catch (e: Exception) {
            println("DeepLinkHandler: Error opening browser: ${e.message}")
            e.printStackTrace()
        }
    }
}

/**
 * Sealed interface representing different types of deep link events
 */
sealed interface DeepLinkEvent {
    /**
     * Authentication callback with tokens from web browser
     */
    data class AuthCallback(
        val accessToken: String,
        val refreshToken: String
    ) : DeepLinkEvent

    /**
     * Unknown deep link received
     */
    data class Unknown(val url: String) : DeepLinkEvent

    /**
     * Error processing deep link
     */
    data class Error(val message: String) : DeepLinkEvent
}
