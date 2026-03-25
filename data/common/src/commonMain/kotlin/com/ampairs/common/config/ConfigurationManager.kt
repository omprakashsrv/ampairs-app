package com.ampairs.common.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized configuration manager for the Ampairs application.
 * Provides a single source of truth for all configuration values.
 */
object ConfigurationManager {

    private val _config = MutableStateFlow(PlatformConfig.getAppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    /**
     * Current configuration instance
     */
    val current: AppConfig
        get() = _config.value

    /**
     * Update configuration (useful for runtime changes or testing)
     */
    fun updateConfig(newConfig: AppConfig) {
        _config.value = newConfig
    }

    /**
     * Reset to platform default configuration
     */
    fun resetToDefault() {
        _config.value = PlatformConfig.getAppConfig()
    }

    /**
     * Quick access properties
     */
    val apiBaseUrl: String
        get() = current.apiBaseUrl

    val environment: Environment
        get() = current.environment

    val isDebug: Boolean
        get() = current.isDebug

    val isDev: Boolean
        get() = current.environment == Environment.DEV

    val isProduction: Boolean
        get() = current.environment == Environment.PRODUCTION

    /**
     * Web authentication URL for desktop browser-based authentication
     */
    val webAuthUrl: String
        get() = current.webAuthUrl

    /**
     * Build complete API URL
     */
    fun getApiUrl(path: String): String = current.getApiUrl(path)

    /**
     * Log current configuration (useful for debugging)
     */
    fun logCurrentConfig(): String {
        return buildString {
            appendLine("=== Ampairs Configuration ===")
            appendLine("Environment: ${current.environment}")
            appendLine("API Base URL: ${current.apiBaseUrl}")
            appendLine("WebSocket URL: ${current.wsBaseUrl}")
            appendLine("Web Auth URL: ${current.webAuthUrl}")
            appendLine("Debug Mode: ${current.isDebug}")
            appendLine("============================")
        }
    }
}