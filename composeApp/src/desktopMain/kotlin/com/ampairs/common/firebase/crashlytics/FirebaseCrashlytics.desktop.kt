package com.ampairs.common.firebase.crashlytics

/**
 * Desktop stub implementation of FirebaseCrashlytics
 * Note: Firebase is not supported on Desktop platforms
 * All methods are no-ops
 */
actual class FirebaseCrashlytics {
    actual fun recordException(throwable: Throwable) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseCrashlytics (Desktop stub): recordException(${throwable.message})")
    }

    actual fun log(message: String) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseCrashlytics (Desktop stub): log($message)")
    }

    actual fun setCustomKey(key: String, value: String) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun setCustomKey(key: String, value: Boolean) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun setCustomKey(key: String, value: Int) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun setCustomKey(key: String, value: Long) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun setCustomKey(key: String, value: Float) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun setCustomKey(key: String, value: Double) {
        // No-op: Firebase not supported on Desktop
    }

    actual fun setUserId(userId: String) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseCrashlytics (Desktop stub): setUserId($userId)")
    }

    actual fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        // No-op: Firebase not supported on Desktop
        println("FirebaseCrashlytics (Desktop stub): setCrashlyticsCollectionEnabled($enabled)")
    }

    actual fun checkForUnsentReports(): Boolean {
        // No-op: Firebase not supported on Desktop
        return false
    }

    actual fun sendUnsentReports() {
        // No-op: Firebase not supported on Desktop
    }

    actual fun deleteUnsentReports() {
        // No-op: Firebase not supported on Desktop
    }
}
