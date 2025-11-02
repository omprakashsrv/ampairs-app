package com.ampairs.common.firebase.messaging

import cocoapods.FirebaseMessaging.FIRMessaging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS implementation of FirebaseMessaging
 */
@OptIn(ExperimentalForeignApi::class)
actual class FirebaseMessaging {
    private val messaging = FIRMessaging.messaging()
    private var tokenRefreshListener: ((String) -> Unit)? = null
    private var messageReceivedListener: ((RemoteMessage) -> Unit)? = null

    actual suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        messaging.tokenWithCompletion { token, error ->
            if (error != null) {
                continuation.resume(null)
            } else {
                continuation.resume(token)
            }
        }
    }

    actual suspend fun deleteToken() = suspendCancellableCoroutine { continuation ->
        messaging.deleteTokenWithCompletion { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    actual suspend fun subscribeToTopic(topic: String) = suspendCancellableCoroutine { continuation ->
        messaging.subscribeToTopic(topic) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    actual suspend fun unsubscribeFromTopic(topic: String) = suspendCancellableCoroutine { continuation ->
        messaging.unsubscribeFromTopic(topic) { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    actual fun isAutoInitEnabled(): Boolean {
        return messaging.isAutoInitEnabled()
    }

    actual fun setAutoInitEnabled(enabled: Boolean) {
        messaging.setAutoInitEnabled(enabled)
    }

    actual fun setOnTokenRefreshListener(onNewToken: (String) -> Unit) {
        tokenRefreshListener = onNewToken
    }

    actual fun setOnMessageReceivedListener(onMessageReceived: (RemoteMessage) -> Unit) {
        messageReceivedListener = onMessageReceived
    }

    /**
     * Called when a new token is received
     * This should be called from your AppDelegate's didReceiveRegistrationToken
     */
    fun onTokenRefreshed(token: String) {
        tokenRefreshListener?.invoke(token)
    }

    /**
     * Called when a message is received
     * This should be called from your AppDelegate's didReceiveRemoteNotification
     */
    fun onMessageReceived(userInfo: Map<Any?, *>) {
        val message = RemoteMessage(
            messageId = userInfo["gcm.message_id"] as? String,
            from = userInfo["from"] as? String,
            to = userInfo["to"] as? String,
            data = (userInfo.filterKeys { it is String } as? Map<String, String>) ?: emptyMap(),
            notification = extractNotification(userInfo)
        )
        messageReceivedListener?.invoke(message)
    }

    private fun extractNotification(userInfo: Map<Any?, *>): RemoteMessage.Notification? {
        val aps = userInfo["aps"] as? Map<*, *> ?: return null
        val alert = aps["alert"] as? Map<*, *> ?: return null

        return RemoteMessage.Notification(
            title = alert["title"] as? String,
            body = alert["body"] as? String,
            imageUrl = userInfo["image"] as? String,
            channelId = null // iOS doesn't have channel IDs
        )
    }
}
