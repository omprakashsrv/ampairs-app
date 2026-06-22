package com.ampairs.inventory.util

import co.touchlab.kermit.Logger

object InventoryLogger {
    fun i(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.i(exception, tag) { message } else Logger.i(tag) { message }
    }

    fun w(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.w(exception, tag) { message } else Logger.w(tag) { message }
    }

    fun e(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.e(exception, tag) { message } else Logger.e(tag) { message }
    }

    fun d(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) Logger.d(exception, tag) { message } else Logger.d(tag) { message }
    }
}
