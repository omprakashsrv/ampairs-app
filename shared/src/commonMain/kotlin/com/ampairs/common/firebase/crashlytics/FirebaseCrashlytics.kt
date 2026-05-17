package com.ampairs.common.firebase.crashlytics

/**
 * Common interface for Firebase Crashlytics across platforms
 */
expect class FirebaseCrashlytics {
    /**
     * Record a non-fatal exception
     * @param throwable The exception to record
     */
    fun recordException(throwable: Throwable)

    /**
     * Log a message that will be sent with the next crash report
     * @param message The message to log
     */
    fun log(message: String)

    /**
     * Set a custom key-value pair that will be associated with crash reports
     * @param key The key for the custom data
     * @param value The value for the custom data
     */
    fun setCustomKey(key: String, value: String)

    /**
     * Set a custom key-value pair that will be associated with crash reports
     * @param key The key for the custom data
     * @param value The boolean value for the custom data
     */
    fun setCustomKey(key: String, value: Boolean)

    /**
     * Set a custom key-value pair that will be associated with crash reports
     * @param key The key for the custom data
     * @param value The numeric value for the custom data
     */
    fun setCustomKey(key: String, value: Int)

    /**
     * Set a custom key-value pair that will be associated with crash reports
     * @param key The key for the custom data
     * @param value The numeric value for the custom data
     */
    fun setCustomKey(key: String, value: Long)

    /**
     * Set a custom key-value pair that will be associated with crash reports
     * @param key The key for the custom data
     * @param value The numeric value for the custom data
     */
    fun setCustomKey(key: String, value: Float)

    /**
     * Set a custom key-value pair that will be associated with crash reports
     * @param key The key for the custom data
     * @param value The numeric value for the custom data
     */
    fun setCustomKey(key: String, value: Double)

    /**
     * Set the user identifier to be associated with crash reports
     * @param userId The user identifier
     */
    fun setUserId(userId: String)

    /**
     * Enable or disable crash reporting
     * @param enabled Whether crash reporting should be enabled
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean)

    /**
     * Check if there are any unsent crash reports
     * @return True if there are unsent reports
     */
    fun checkForUnsentReports(): Boolean

    /**
     * Send any unsent crash reports
     */
    fun sendUnsentReports()

    /**
     * Delete any unsent crash reports
     */
    fun deleteUnsentReports()
}

/**
 * Common Crashlytics Key Names
 */
object CrashlyticsKeys {
    const val USER_ID = "user_id"
    const val WORKSPACE_ID = "workspace_id"
    const val SCREEN_NAME = "screen_name"
    const val ACTION = "action"
    const val ERROR_TYPE = "error_type"
    const val NETWORK_STATUS = "network_status"
    const val APP_VERSION = "app_version"
    const val BUILD_NUMBER = "build_number"
}
