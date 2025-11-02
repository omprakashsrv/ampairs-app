package com.ampairs.common.config

import com.ampairs.app.BuildConfig

/**
 * Android-specific configuration implementation
 * Uses BuildConfig values generated during build time
 */
actual object PlatformConfig {
    actual fun getAppConfig(): AppConfig {
        val environment = Environment.fromString(BuildConfig.ENVIRONMENT)

        return AppConfig(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            environment = environment,
            isDebug = BuildConfig.DEBUG
        )
    }
}