package com.ampairs.customer.util

import co.touchlab.kermit.Logger
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

object CustomerLogger {

    private const val TAG = "Customer"

    fun info(message: String) {
        Logger.i(TAG) { message }
    }

    fun i(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.i(exception, tag) { message }
        else Logger.i(tag) { message }
    }

    fun warn(message: String) {
        Logger.w(TAG) { message }
        ErrorTracking.captureMessage("[$TAG] $message", SentryLevel.WARNING)
    }

    fun w(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.w(exception, tag) { message }
        else Logger.w(tag) { message }
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.WARNING)
    }

    fun error(message: String) {
        Logger.e(TAG) { message }
        ErrorTracking.captureMessage("[$TAG] $message", SentryLevel.ERROR)
    }

    fun e(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.e(exception, tag) { message }
        else Logger.e(tag) { message }
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.ERROR)
    }

    fun debug(message: String) {
        Logger.d(TAG) { message }
    }

    fun d(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.d(exception, tag) { message }
        else Logger.d(tag) { message }
    }
}
