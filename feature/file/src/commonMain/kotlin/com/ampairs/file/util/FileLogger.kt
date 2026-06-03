package com.ampairs.file.util

import co.touchlab.kermit.Logger

object FileLogger {
    fun d(tag: String, message: String) = Logger.d(tag) { message }
    fun i(tag: String, message: String) = Logger.i(tag) { message }
    fun w(tag: String, message: String, throwable: Throwable? = null) =
        Logger.w(throwable, tag) { message }
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        Logger.e(throwable, tag) { message }
}
