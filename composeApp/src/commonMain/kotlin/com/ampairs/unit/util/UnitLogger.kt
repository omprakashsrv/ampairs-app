package com.ampairs.unit.util

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/**
 * Simple KMP-compatible logger for Unit Management Module
 *
 * Uses println for console output and Sentry for error tracking.
 * Error and warning messages with exceptions are automatically sent to Sentry.
 */
object UnitLogger {

    private const val TAG = "Unit"

    /**
     * Log debug messages
     */
    fun d(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] DEBUG: $message$exceptionMsg")
    }

    /**
     * Log informational messages
     */
    fun i(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] INFO: $message$exceptionMsg")
    }

    /**
     * Log warning messages with tag and exception
     * Exceptions are automatically reported to Sentry
     */
    fun w(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] WARN: $message$exceptionMsg")
        // Report exception to Sentry if present
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.WARNING)
    }

    /**
     * Log error messages with tag and exception
     * Exceptions are automatically reported to Sentry
     */
    fun e(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] ERROR: $message$exceptionMsg")
        // Report exception to Sentry if present
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.ERROR)
    }
}
