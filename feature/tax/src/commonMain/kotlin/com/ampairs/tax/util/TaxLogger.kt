package com.ampairs.tax.util

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/**
 * KMP-compatible logger for the Tax module.
 *
 * Mirrors the other feature loggers (e.g. UnitLogger): console output for d/i, and w/e additionally
 * report to Sentry. Use this instead of `println` so production logging is captured and leveled.
 */
object TaxLogger {

    fun d(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] DEBUG: $message$exceptionMsg")
    }

    fun i(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] INFO: $message$exceptionMsg")
    }

    fun w(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] WARN: $message$exceptionMsg")
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.WARNING)
    }

    fun e(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] ERROR: $message$exceptionMsg")
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.ERROR)
    }
}
