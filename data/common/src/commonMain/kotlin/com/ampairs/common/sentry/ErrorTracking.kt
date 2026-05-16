package com.ampairs.common.sentry

/**
 * Error tracking utility for easy Sentry integration.
 * Provides convenient wrapper methods around SentryManager with safe error handling.
 */
object ErrorTracking {

    /**
     * Capture an exception and send it to Sentry.
     * @param throwable The exception to capture
     * @param tag Optional tag for categorizing the error
     */
    fun captureException(throwable: Throwable, tag: String? = null) {
        try {
            SentryManager.captureException(throwable, tag)
        } catch (e: Exception) {
            // Fail silently to avoid crashing the app if Sentry fails
            println("ErrorTracking: Failed to capture exception - ${e.message}")
        }
    }

    /**
     * Capture an error message and send it to Sentry.
     * @param message The error message to capture
     * @param level The severity level
     */
    fun captureMessage(message: String, level: SentryLevel = SentryLevel.ERROR) {
        try {
            SentryManager.captureMessage(message, level)
        } catch (e: Exception) {
            println("ErrorTracking: Failed to capture message - ${e.message}")
        }
    }

    /**
     * Set user information for error context.
     * @param userId User identifier
     * @param username Optional username
     * @param email Optional email
     */
    fun setUser(userId: String?, username: String? = null, email: String? = null) {
        try {
            SentryManager.setUser(userId, username, email)
        } catch (e: Exception) {
            println("ErrorTracking: Failed to set user - ${e.message}")
        }
    }

    /**
     * Clear user information.
     */
    fun clearUser() {
        try {
            SentryManager.clearUser()
        } catch (e: Exception) {
            println("ErrorTracking: Failed to clear user - ${e.message}")
        }
    }

    /**
     * Add a breadcrumb for debugging context.
     * @param message The breadcrumb message
     * @param category Optional category
     */
    fun addBreadcrumb(message: String, category: String? = null) {
        try {
            SentryManager.addBreadcrumb(message, category)
        } catch (e: Exception) {
            println("ErrorTracking: Failed to add breadcrumb - ${e.message}")
        }
    }

    /**
     * Log debug information and add as breadcrumb.
     */
    fun logDebug(message: String, category: String? = null) {
        addBreadcrumb(message, category)
        println("DEBUG [$category]: $message")
    }

    /**
     * Log info and add as breadcrumb.
     */
    fun logInfo(message: String, category: String? = null) {
        addBreadcrumb(message, category)
        println("INFO [$category]: $message")
    }
}

/**
 * Extension function for Throwable to easily report to Sentry.
 * @param tag Optional tag for categorizing the error
 */
fun Throwable.reportToSentry(tag: String? = null) {
    ErrorTracking.captureException(this, tag)
}

/**
 * Extension function for Result to report failures to Sentry.
 * @param tag Optional tag for categorizing the error
 */
inline fun <T> Result<T>.reportFailureToSentry(tag: String? = null): Result<T> {
    this.exceptionOrNull()?.let { exception ->
        ErrorTracking.captureException(exception, tag)
    }
    return this
}

/**
 * Safe execution wrapper that catches exceptions and reports to Sentry.
 * @param tag Optional tag for categorizing errors
 * @param onError Optional error handler
 * @param block The code to execute
 * @return Result of the execution
 */
inline fun <T> trySentry(
    tag: String? = null,
    noinline onError: ((Throwable) -> T)? = null,
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        ErrorTracking.captureException(e, tag)
        onError?.invoke(e)
    }
}
