package com.ampairs.sequence.util

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/**
 * Simple KMP-compatible logger for the Sequence module.
 * Mirrors UnitLogger: println for console output, Sentry for warnings/errors.
 */
object SequenceLogger {

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
