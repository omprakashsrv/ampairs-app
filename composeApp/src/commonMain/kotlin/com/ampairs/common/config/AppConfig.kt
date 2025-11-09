package com.ampairs.common.config

/**
 * Environment configuration for the Ampairs application.
 * Provides platform-specific API base URLs and environment settings.
 */
data class AppConfig(
    val apiBaseUrl: String,
    val environment: Environment,
    val isDebug: Boolean = environment == Environment.DEV
) {

    /**
     * Complete API endpoint URL with path
     */
    fun getApiUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "$apiBaseUrl/api/v1/$cleanPath"
    }

    /**
     * WebSocket URL for real-time updates
     */
    val wsBaseUrl: String
        get() = apiBaseUrl.replace("http://", "ws://").replace("https://", "wss://")

    /**
     * Web authentication URL for desktop browser-based authentication
     * Maps to the web application's login page
     */
    val webAuthUrl: String
        get() = when (environment) {
            Environment.DEV -> "http://localhost:4200/login"
            Environment.PRODUCTION -> "https://app.ampairs.com/login"
        }

    companion object {
        /**
         * Get IP address for mobile platforms (Android/iOS)
         * This should be your development machine's IP address
         */
        const val DEV_MOBILE_IP = "10.50.51.5" // Change this to your actual IP

        /**
         * Default ports for different environments
         */
        const val DEV_PORT = 8080
        const val PROD_PORT = 443
    }
}

/**
 * Application environment types
 */
enum class Environment {
    DEV,
    PRODUCTION;

    companion object {
        fun fromString(value: String): Environment {
            return when (value.lowercase()) {
                "dev", "development", "debug" -> DEV
                "prod", "production", "release" -> PRODUCTION
                else -> DEV
            }
        }
    }
}

/**
 * Platform-specific configuration provider
 * Each platform (Android, iOS, Desktop) implements this differently
 */
expect object PlatformConfig {
    fun getAppConfig(): AppConfig
}