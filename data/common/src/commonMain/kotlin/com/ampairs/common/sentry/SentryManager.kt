package com.ampairs.common.sentry

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel as SdkSentryLevel

/**
 * Shared Sentry manager for all platforms using official Sentry KMP SDK.
 *
 * This implementation follows the official Sentry Compose Multiplatform documentation:
 * https://docs.sentry.io/platforms/kotlin/guides/compose-multiplatform/
 *
 * Note: The Sentry KMP SDK (v0.21.0) has a limited API compared to platform-specific SDKs.
 * Some features like User and Breadcrumb require platform-specific implementations.
 */
object SentryManager {
    /**
     * Initialize Sentry with platform-agnostic configuration.
     * Call this early in your application's lifecycle.
     *
     * @param dsn The Sentry Data Source Name (DSN) for error reporting
     * @param environment The environment name (e.g., "dev", "production")
     * @param enableDebug Whether to enable debug logging for Sentry
     */
    fun initialize(
        dsn: String,
        environment: String,
        enableDebug: Boolean = false
    ) {
        Sentry.init { options ->
            options.dsn = dsn
            options.environment = environment
            options.debug = enableDebug
            options.sendDefaultPii = true
        }
    }

    /**
     * Capture an exception and send it to Sentry.
     * @param throwable The exception to capture
     * @param tag Optional tag for categorizing the error
     */
    fun captureException(throwable: Throwable, tag: String? = null) {
        try {
            if (tag != null) {
                Sentry.configureScope { scope ->
                    scope.setTag("custom_tag", tag)
                }
            }
            Sentry.captureException(throwable)
        } catch (e: Exception) {
            println("Failed to capture exception to Sentry: ${e.message}")
        }
    }

    /**
     * Capture a message and send it to Sentry.
     * @param message The message to capture
     * @param level The severity level (debug, info, warning, error, fatal)
     */
    fun captureMessage(message: String, level: SentryLevel = SentryLevel.INFO) {
        try {
            val sdkLevel = when (level) {
                SentryLevel.DEBUG -> SdkSentryLevel.DEBUG
                SentryLevel.INFO -> SdkSentryLevel.INFO
                SentryLevel.WARNING -> SdkSentryLevel.WARNING
                SentryLevel.ERROR -> SdkSentryLevel.ERROR
                SentryLevel.FATAL -> SdkSentryLevel.FATAL
            }
            Sentry.configureScope { scope ->
                scope.level = sdkLevel
            }
            Sentry.captureMessage(message)
        } catch (e: Exception) {
            println("Failed to capture message to Sentry: ${e.message}")
        }
    }

    /**
     * Set user information for error context.
     * Note: User API not available in Sentry KMP v0.21.0 - using tags as workaround.
     *
     * @param userId User identifier
     * @param username Optional username
     * @param email Optional email
     */
    fun setUser(userId: String?, username: String? = null, email: String? = null) {
        try {
            Sentry.configureScope { scope ->
                userId?.let { scope.setTag("user_id", it) }
                username?.let { scope.setTag("username", it) }
                email?.let { scope.setTag("user_email", it) }
            }
        } catch (e: Exception) {
            println("Failed to set user in Sentry: ${e.message}")
        }
    }

    /**
     * Clear user information.
     */
    fun clearUser() {
        try {
            Sentry.configureScope { scope ->
                scope.removeTag("user_id")
                scope.removeTag("username")
                scope.removeTag("user_email")
            }
        } catch (e: Exception) {
            println("Failed to clear user in Sentry: ${e.message}")
        }
    }

    /**
     * Add a breadcrumb for debugging context.
     * Note: Breadcrumb API not available in Sentry KMP v0.21.0 - using tags as workaround.
     *
     * @param message The breadcrumb message
     * @param category Optional category
     */
    fun addBreadcrumb(message: String, category: String? = null) {
        try {
            // Store as tag since breadcrumb API not available
            val breadcrumbKey = "breadcrumb_${category ?: "general"}"
            Sentry.configureScope { scope ->
                scope.setTag(breadcrumbKey, message)
            }
        } catch (e: Exception) {
            println("Failed to add breadcrumb to Sentry: ${e.message}")
        }
    }
}

/**
 * Sentry severity levels for messages.
 */
enum class SentryLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL
}
