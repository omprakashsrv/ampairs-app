package com.ampairs.event.util

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/**
 * Simple logger for event module.
 * Error and warning messages with exceptions are automatically sent to Sentry.
 */
object EventLogger {
    private const val TAG = "Event"

    fun d(component: String, message: String) {
        println("[$TAG][$component] DEBUG: $message")
    }

    fun i(component: String, message: String) {
        println("[$TAG][$component] INFO: $message")
    }

    fun w(component: String, message: String, exception: Throwable? = null) {
        println("[$TAG][$component] WARN: $message")
        exception?.let {
            println("[$TAG][$component] Exception: ${it.message}")
            // Report exception to Sentry
            ErrorTracking.captureException(it, "$TAG.$component")
        } ?: ErrorTracking.captureMessage("[$TAG][$component] $message", SentryLevel.WARNING)
    }

    fun e(component: String, message: String, exception: Throwable? = null) {
        println("[$TAG][$component] ERROR: $message")
        exception?.let {
            println("[$TAG][$component] Exception: ${it.message}")
            // Report exception to Sentry
            ErrorTracking.captureException(it, "$TAG.$component")
        } ?: ErrorTracking.captureMessage("[$TAG][$component] $message", SentryLevel.ERROR)
    }
}
