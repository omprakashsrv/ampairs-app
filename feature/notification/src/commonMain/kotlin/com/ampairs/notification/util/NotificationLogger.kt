package com.ampairs.notification.util

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/**
 * KMP-compatible logger for the Notification module.
 *
 * Console output via println; warnings/errors with exceptions are reported to Sentry.
 * Signature mirrors the project convention: `w/e/i/d(tag, message, exception?)`.
 */
object NotificationLogger {

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
