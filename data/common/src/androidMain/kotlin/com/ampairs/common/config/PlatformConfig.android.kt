package com.ampairs.common.config

/**
 * Android-specific configuration implementation.
 * Call [configure] from the app module (where BuildConfig is available) before first use.
 */
actual object PlatformConfig {
    private var configured: AppConfig? = null

    /**
     * Called from composeApp/androidMain with BuildConfig values before Koin initializes.
     */
    fun configure(apiBaseUrl: String, environment: String, isDebug: Boolean) {
        configured = AppConfig(
            apiBaseUrl = apiBaseUrl,
            environment = Environment.fromString(environment),
            isDebug = isDebug
        )
    }

    actual fun getAppConfig(): AppConfig {
        return configured ?: AppConfig(
            apiBaseUrl = "http://10.50.51.11:8080",
            environment = Environment.DEV,
            isDebug = true
        )
    }
}