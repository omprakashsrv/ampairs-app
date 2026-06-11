package com.ampairs.form

import co.touchlab.kermit.Logger
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.sentry.SentryLevel

/** Default batch size for form sync operations. */
const val FORM_SYNC_BATCH_SIZE: Int = 100

/** Domain logger for the form module — 3-param `i/w/e/d` signature, consistent with other features. */
object FormLogger {

    private const val TAG = "Form"

    fun i(tag: String = TAG, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.i(exception, tag) { message } else Logger.i(tag) { message }
    }

    fun d(tag: String = TAG, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.d(exception, tag) { message } else Logger.d(tag) { message }
    }

    fun w(tag: String = TAG, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.w(exception, tag) { message } else Logger.w(tag) { message }
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.WARNING)
    }

    fun e(tag: String = TAG, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.e(exception, tag) { message } else Logger.e(tag) { message }
        exception?.let { ErrorTracking.captureException(it, tag) }
            ?: ErrorTracking.captureMessage("[$tag] $message", SentryLevel.ERROR)
    }
}
