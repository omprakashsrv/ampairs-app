package com.ampairs.event.util

/**
 * Simple logger for event module.
 * Can be replaced with more sophisticated logging (e.g., Kermit) later.
 */
object EventLogger {
    private const val TAG = "Event"

    fun d(component: String, message: String) {
        println("[$TAG][$component] DEBUG: $message")
    }

    fun i(component: String, message: String) {
        println("[$TAG][$component] INFO: $message")
    }

    fun w(component: String, message: String, exception: Throwable? = null) {
        println("[$TAG][$component] WARN: $message")
        exception?.let {
            println("[$TAG][$component] Exception: ${it.message}")
            it.printStackTrace()
        }
    }

    fun e(component: String, message: String, exception: Throwable? = null) {
        println("[$TAG][$component] ERROR: $message")
        exception?.let {
            println("[$TAG][$component] Exception: ${it.message}")
            it.printStackTrace()
        }
    }
}
