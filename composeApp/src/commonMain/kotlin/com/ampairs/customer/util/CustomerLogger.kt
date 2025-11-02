package com.ampairs.customer.util

/**
 * Simple KMP-compatible logger for Customer module
 * Replaces println statements with structured logging
 */
object CustomerLogger {

    private const val TAG = "Customer"

    /**
     * Log informational messages
     */
    fun info(message: String) {
        println("[$TAG] INFO: $message")
    }

    /**
     * Log informational messages with tag and exception
     */
    fun i(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] INFO: $message$exceptionMsg")
    }

    /**
     * Log warning messages
     */
    fun warn(message: String) {
        println("[$TAG] WARN: $message")
    }

    /**
     * Log warning messages with tag and exception
     */
    fun w(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] WARN: $message$exceptionMsg")
    }

    /**
     * Log error messages
     */
    fun error(message: String) {
        println("[$TAG] ERROR: $message")
    }

    /**
     * Log error messages with tag and exception
     */
    fun e(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] ERROR: $message$exceptionMsg")
    }

    /**
     * Log debug messages - typically used for development
     */
    fun debug(message: String) {
        println("[$TAG] DEBUG: $message")
    }

    /**
     * Log debug messages with tag and exception
     */
    fun d(tag: String, message: String, exception: Throwable? = null) {
        val exceptionMsg = exception?.let { " - ${it.message}" } ?: ""
        println("[$tag] DEBUG: $message$exceptionMsg")
    }
}