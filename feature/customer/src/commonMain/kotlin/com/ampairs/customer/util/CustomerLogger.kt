package com.ampairs.customer.util

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/**
 * Simple KMP-compatible logger for Customer module
 * Replaces println statements with structured logging.
 * Error and warning messages with exceptions are automatically sent to Sentry.
 */
object CustomerLogger {

    private const val TAG = "Customer"

    /**
     * Log informational messages
     */
    fun info(message: String) {
        println("[$TAG] INFO: $message")
    }

    /**
     * Log informational messages with tag and exception
     */
    fun i(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] INFO: $message$exceptionMsg")
    }

    /**
     * Log warning messages
     */
    fun warn(message: String) {
        println("[$TAG] WARN: $message")
        // Send warning messages to Sentry for monitoring
        ErrorTracking.captureMessage("[$TAG] $message", SentryLevel.WARNING)
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
     * Log error messages
     * Error messages are automatically sent to Sentry
     */
    fun error(message: String) {
        println("[$TAG] ERROR: $message")
        // Send error messages to Sentry
        ErrorTracking.captureMessage("[$TAG] $message", SentryLevel.ERROR)
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

    /**
     * Log debug messages - typically used for development
     */
    fun debug(message: String) {
        println("[$TAG] DEBUG: $message")
    }

    /**
     * Log debug messages with tag and exception
     */
    fun d(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] DEBUG: $message$exceptionMsg")
    }
}