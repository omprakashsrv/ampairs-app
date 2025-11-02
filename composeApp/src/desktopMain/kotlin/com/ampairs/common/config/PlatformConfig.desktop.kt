package com.ampairs.common.config

/**
 * Desktop-specific configuration implementation
 * Uses system properties and environment variables for configuration
 *
 * Environment variables:
 * - AMPAIRS_ENVIRONMENT: dev|production (default: dev)
 * - AMPAIRS_API_BASE_URL: API base URL (overrides default)
 * - AMPAIRS_WEB_AUTH_URL: Web authentication URL (overrides default)
 *
 * System properties (higher priority):
 * - ampairs.environment
 * - ampairs.api.baseUrl
 * - ampairs.web.authUrl
 */
actual object PlatformConfig {
    actual fun getAppConfig(): AppConfig {
        // Check system properties first, then environment variables, then defaults
        val envProperty = System.getProperty("ampairs.environment")
            ?: System.getenv("AMPAIRS_ENVIRONMENT")
            ?: "dev"

        val environment = Environment.fromString(envProperty)

        val apiBaseUrl = when (environment) {
            Environment.DEV -> {
                // For desktop dev, use localhost by default
                System.getProperty("ampairs.api.baseUrl")
                    ?: System.getenv("AMPAIRS_API_BASE_URL")
                    ?: "http://localhost:${AppConfig.DEV_PORT}"
            }
            Environment.PRODUCTION -> {
                System.getProperty("ampairs.api.baseUrl")
                    ?: System.getenv("AMPAIRS_API_BASE_URL")
                    ?: "https://api.ampairs.com"
            }
        }

        return AppConfig(
            apiBaseUrl = apiBaseUrl,
            environment = environment,
            isDebug = environment == Environment.DEV
        )
    }
}