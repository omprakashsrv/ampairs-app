package com.ampairs.common.firebase.messaging

import com.google.firebase.messaging.FirebaseMessaging as AndroidFirebaseMessaging
import com.google.firebase.messaging.RemoteMessage as AndroidRemoteMessage
import kotlinx.coroutines.tasks.await

/**
 * Android implementation of FirebaseMessaging
 */
actual class FirebaseMessaging {
    private val messaging = AndroidFirebaseMessaging.getInstance()
    private var tokenRefreshListener: ((String) -> Unit)? = null
    private var messageReceivedListener: ((RemoteMessage) -> Unit)? = null

    actual suspend fun getToken(): String? {
        return try {
            messaging.token.await()
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun deleteToken() {
        try {
            messaging.deleteToken().await()
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    actual suspend fun subscribeToTopic(topic: String) {
        try {
            messaging.subscribeToTopic(topic).await()
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    actual suspend fun unsubscribeFromTopic(topic: String) {
        try {
            messaging.unsubscribeFromTopic(topic).await()
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    actual fun isAutoInitEnabled(): Boolean {
        return messaging.isAutoInitEnabled
    }

    actual fun setAutoInitEnabled(enabled: Boolean) {
        messaging.isAutoInitEnabled = enabled
    }

    actual fun setOnTokenRefreshListener(onNewToken: (String) -> Unit) {
        tokenRefreshListener = onNewToken
    }

    actual fun setOnMessageReceivedListener(onMessageReceived: (RemoteMessage) -> Unit) {
        messageReceivedListener = onMessageReceived
    }

    /**
     * Called from FirebaseMessagingService when a new token is received
     * This should be called from your custom FirebaseMessagingService
     */
    fun onTokenRefreshed(token: String) {
        tokenRefreshListener?.invoke(token)
    }

    /**
     * Called from FirebaseMessagingService when a message is received
     * This should be called from your custom FirebaseMessagingService
     */
    fun onMessageReceived(message: AndroidRemoteMessage) {
        messageReceivedListener?.invoke(message.toCommonRemoteMessage())
    }
}

/**
 * Extension function to convert Android RemoteMessage to common RemoteMessage
 */
private fun AndroidRemoteMessage.toCommonRemoteMessage(): RemoteMessage {
    return RemoteMessage(
        messageId = messageId,
        from = from,
        to = to,
        data = data,
        notification = notification?.let {
            RemoteMessage.Notification(
                title = it.title,
                body = it.body,
                imageUrl = it.imageUrl?.toString(),
                channelId = it.channelId
            )
        }
    )
}
