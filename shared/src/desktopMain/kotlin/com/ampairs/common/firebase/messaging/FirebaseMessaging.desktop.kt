package com.ampairs.common.firebase.messaging

/**
 * Desktop stub implementation of FirebaseMessaging
 * Note: Firebase Cloud Messaging is not supported on Desktop platforms
 * All methods are no-ops or return safe defaults
 */
actual class FirebaseMessaging {
    private var tokenRefreshListener: ((String) -> Unit)? = null
    private var messageReceivedListener: ((RemoteMessage) -> Unit)? = null

    actual suspend fun getToken(): String? {
        // No-op: FCM not supported on Desktop
        println("FirebaseMessaging (Desktop stub): getToken()")
        return null
    }

    actual suspend fun deleteToken() {
        // No-op: FCM not supported on Desktop
        println("FirebaseMessaging (Desktop stub): deleteToken()")
    }

    actual suspend fun subscribeToTopic(topic: String) {
        // No-op: FCM not supported on Desktop
        println("FirebaseMessaging (Desktop stub): subscribeToTopic($topic)")
    }

    actual suspend fun unsubscribeFromTopic(topic: String) {
        // No-op: FCM not supported on Desktop
        println("FirebaseMessaging (Desktop stub): unsubscribeFromTopic($topic)")
    }

    actual fun isAutoInitEnabled(): Boolean {
        // No-op: FCM not supported on Desktop
        return false
    }

    actual fun setAutoInitEnabled(enabled: Boolean) {
        // No-op: FCM not supported on Desktop
        println("FirebaseMessaging (Desktop stub): setAutoInitEnabled($enabled)")
    }

    actual fun setOnTokenRefreshListener(onNewToken: (String) -> Unit) {
        // Store listener but it will never be called on Desktop
        tokenRefreshListener = onNewToken
        println("FirebaseMessaging (Desktop stub): setOnTokenRefreshListener")
    }

    actual fun setOnMessageReceivedListener(onMessageReceived: (RemoteMessage) -> Unit) {
        // Store listener but it will never be called on Desktop
        messageReceivedListener = onMessageReceived
        println("FirebaseMessaging (Desktop stub): setOnMessageReceivedListener")
    }
}
