package com.ampairs.communication.util

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/**
 * KMP-compatible logger for the communication module. Errors/warnings with exceptions are reported
 * to Sentry. Method names are w/e/i/d (not warn/error) — see project logger rule.
 */
object CommunicationLogger {

    fun d(tag: String, message: String, exception: Throwable? = null) {
        val ex = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] DEBUG: $message$ex")
    }

    fun i(tag: String, message: String, exception: Throwable? = null) {
        val ex = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] INFO: $message$ex")
    }

    fun w(tag: String, message: String, exception: Throwable? = null) {
        val ex = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] WARN: $message$ex")
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.WARNING)
    }

    fun e(tag: String, message: String, exception: Throwable? = null) {
        val ex = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] ERROR: $message$ex")
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.ERROR)
    }
}
