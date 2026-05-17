package com.ampairs.common.firebase.crashlytics

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * Android implementation of FirebaseCrashlytics
 */
actual class FirebaseCrashlytics {
    private val crashlytics = Firebase.crashlytics

    actual fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    actual fun log(message: String) {
        crashlytics.log(message)
    }

    actual fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Long) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Float) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Double) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    actual fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    actual fun checkForUnsentReports(): Boolean {
        // Note: This is not directly available in Android Firebase SDK
        // We return false as a safe default
        return false
    }

    actual fun sendUnsentReports() {
        crashlytics.sendUnsentReports()
    }

    actual fun deleteUnsentReports() {
        crashlytics.deleteUnsentReports()
    }
}
