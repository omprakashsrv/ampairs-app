package com.ampairs.common.config

import platform.Foundation.NSBundle

/**
 * iOS-specific configuration implementation
 * Uses Info.plist values and runtime configuration
 */
actual object PlatformConfig {
    actual fun getAppConfig(): AppConfig {
        // Get configuration from Info.plist or use defaults
        val bundle = NSBundle.mainBundle

        val envString = bundle.objectForInfoDictionaryKey("AMPAIRS_ENVIRONMENT") as? String
            ?: "dev"

        val environment = Environment.fromString(envString)

        val apiBaseUrl = when (environment) {
            Environment.DEV -> {
                // For iOS dev, use the mobile IP address
                bundle.objectForInfoDictionaryKey("AMPAIRS_API_BASE_URL") as? String
                    ?: "http://${AppConfig.DEV_MOBILE_IP}:${AppConfig.DEV_PORT}"
            }
            Environment.PRODUCTION -> {
                bundle.objectForInfoDictionaryKey("AMPAIRS_API_BASE_URL") as? String
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