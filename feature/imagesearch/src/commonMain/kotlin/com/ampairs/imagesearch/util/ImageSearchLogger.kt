package com.ampairs.imagesearch.util

import co.touchlab.kermit.Logger
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/** Domain logger for the image-search module — mirrors the 3-param w/e/i/d convention (see rules). */
object ImageSearchLogger {

    fun i(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.i(exception, tag) { message }
        else Logger.i(tag) { message }
    }

    fun w(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.w(exception, tag) { message }
        else Logger.w(tag) { message }
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.WARNING)
    }

    fun e(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.e(exception, tag) { message }
        else Logger.e(tag) { message }
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.ERROR)
    }

    fun d(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.d(exception, tag) { message }
        else Logger.d(tag) { message }
    }
}
