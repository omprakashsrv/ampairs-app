package com.ampairs.common.firebase.messaging

/**
 * Common interface for Firebase Cloud Messaging (FCM) across platforms
 */
expect class FirebaseMessaging {
    /**
     * Get the FCM registration token
     * @return The current FCM token, or null if not available
     */
    suspend fun getToken(): String?

    /**
     * Delete the FCM registration token
     */
    suspend fun deleteToken()

    /**
     * Subscribe to a topic
     * @param topic The topic name to subscribe to
     */
    suspend fun subscribeToTopic(topic: String)

    /**
     * Unsubscribe from a topic
     * @param topic The topic name to unsubscribe from
     */
    suspend fun unsubscribeFromTopic(topic: String)

    /**
     * Check if auto-initialization is enabled
     * @return True if auto-initialization is enabled
     */
    fun isAutoInitEnabled(): Boolean

    /**
     * Set auto-initialization enabled state
     * @param enabled Whether auto-initialization should be enabled
     */
    fun setAutoInitEnabled(enabled: Boolean)

    /**
     * Set a listener for when a new token is generated
     * @param onNewToken Callback invoked when a new token is received
     */
    fun setOnTokenRefreshListener(onNewToken: (String) -> Unit)

    /**
     * Set a listener for when a message is received while app is in foreground
     * @param onMessageReceived Callback invoked when a message is received
     */
    fun setOnMessageReceivedListener(onMessageReceived: (RemoteMessage) -> Unit)
}

/**
 * Represents a remote message received from FCM
 */
data class RemoteMessage(
    val messageId: String?,
    val from: String?,
    val to: String?,
    val data: Map<String, String>,
    val notification: Notification?
) {
    data class Notification(
        val title: String?,
        val body: String?,
        val imageUrl: String?,
        val channelId: String?
    )
}

/**
 * Common FCM Topic Names
 */
object FcmTopics {
    const val ALL_USERS = "all_users"
    const val WORKSPACE_PREFIX = "workspace_"
    const val ANNOUNCEMENTS = "announcements"
    const val UPDATES = "updates"

    /**
     * Generate a workspace-specific topic name
     * @param workspaceId The workspace ID
     * @return The topic name for the workspace
     */
    fun workspace(workspaceId: String): String = "${WORKSPACE_PREFIX}${workspaceId}"
}

/**
 * Common FCM Data Keys
 */
object FcmDataKeys {
    const val TYPE = "type"
    const val ACTION = "action"
    const val WORKSPACE_ID = "workspace_id"
    const val USER_ID = "user_id"
    const val ENTITY_ID = "entity_id"
    const val TITLE = "title"
    const val MESSAGE = "message"
    const val IMAGE_URL = "image_url"
    const val DEEP_LINK = "deep_link"
}

/**
 * Common FCM Message Types
 */
object FcmMessageTypes {
    const val NOTIFICATION = "notification"
    const val DATA = "data"
    const val CHAT_MESSAGE = "chat_message"
    const val ORDER_UPDATE = "order_update"
    const val INVOICE_UPDATE = "invoice_update"
    const val WORKSPACE_INVITATION = "workspace_invitation"
    const val SYSTEM_ANNOUNCEMENT = "system_announcement"
}
