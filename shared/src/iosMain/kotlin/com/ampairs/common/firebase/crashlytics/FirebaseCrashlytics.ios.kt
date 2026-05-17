package com.ampairs.common.firebase.crashlytics

import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.Foundation.NSException

/**
 * iOS implementation of FirebaseCrashlytics
 */
@OptIn(ExperimentalForeignApi::class)
actual class FirebaseCrashlytics {
    private val crashlytics = FIRCrashlytics.crashlytics()

    actual fun recordException(throwable: Throwable) {
        // Convert Kotlin exception to NSError for iOS
        val nsError = NSError.errorWithDomain(
            domain = "com.ampairs.app",
            code = -1,
            userInfo = mapOf(
                "message" to (throwable.message ?: "Unknown error"),
                "stackTrace" to (throwable.stackTraceToString())
            )
        )
        crashlytics.recordError(nsError)
    }

    actual fun log(message: String) {
        crashlytics.log(message)
    }

    actual fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomValue(value, key)
    }

    actual fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomValue(value, key)
    }

    actual fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomValue(value, key)
    }

    actual fun setCustomKey(key: String, value: Long) {
        crashlytics.setCustomValue(value, key)
    }

    actual fun setCustomKey(key: String, value: Float) {
        crashlytics.setCustomValue(value, key)
    }

    actual fun setCustomKey(key: String, value: Double) {
        crashlytics.setCustomValue(value, key)
    }

    actual fun setUserId(userId: String) {
        crashlytics.setUserID(userId)
    }

    actual fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    actual fun checkForUnsentReports(): Boolean {
        // iOS SDK provides this via a callback, which doesn't fit synchronous API
        // Return false as safe default
        return false
    }

    actual fun sendUnsentReports() {
        crashlytics.sendUnsentReports()
    }

    actual fun deleteUnsentReports() {
        crashlytics.deleteUnsentReports()
    }
}
