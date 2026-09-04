package com.ampairs.cbmaintenance.util

/** Simple KMP-compatible logger for the cb-maintenance feature. */
object CbMaintenanceLogger {
    fun d(tag: String, message: String, exception: Throwable? = null) =
        println("[$tag] DEBUG: $message${exception?.let { " - ${it.message}" } ?: ""}")

    fun i(tag: String, message: String, exception: Throwable? = null) =
        println("[$tag] INFO: $message${exception?.let { " - ${it.message}" } ?: ""}")

    fun w(tag: String, message: String, exception: Throwable? = null) =
        println("[$tag] WARN: $message${exception?.let { " - ${it.message}" } ?: ""}")

    fun e(tag: String, message: String, exception: Throwable? = null) =
        println("[$tag] ERROR: $message${exception?.let { " - ${it.message}" } ?: ""}")
}
