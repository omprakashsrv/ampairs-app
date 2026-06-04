package com.ampairs.ecom.domain

import co.touchlab.kermit.Logger

/**
 * Domain logger for the ecom module. 3-param signature (tag, message, throwable)
 * matching the project convention (w/e/i/d — never warn/error).
 */
object EcomLogger {
    private val log = Logger.withTag("Ecom")

    fun d(tag: String, message: String, throwable: Throwable? = null) = log.d(throwable) { "[$tag] $message" }
    fun i(tag: String, message: String, throwable: Throwable? = null) = log.i(throwable) { "[$tag] $message" }
    fun w(tag: String, message: String, throwable: Throwable? = null) = log.w(throwable) { "[$tag] $message" }
    fun e(tag: String, message: String, throwable: Throwable? = null) = log.e(throwable) { "[$tag] $message" }
}
