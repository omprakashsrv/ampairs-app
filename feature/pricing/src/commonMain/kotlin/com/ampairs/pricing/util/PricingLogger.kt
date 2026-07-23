package com.ampairs.pricing.util

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/** KMP-compatible logger for the Pricing module. Errors/warnings with exceptions go to Sentry. */
object PricingLogger {

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
