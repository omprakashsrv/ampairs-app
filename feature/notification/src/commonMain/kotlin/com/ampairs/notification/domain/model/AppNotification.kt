package com.ampairs.notification.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * In-app notification (notification feed entry).
 *
 * Doubles as the wire DTO for the canonical `/v1/notifications/feed/sync` contract — the backend
 * `NotificationLogResponse` / `NotificationLogRequest` field names map 1:1 here via `@SerialName`.
 * The backend `id` is the stable uid (client UID-keyed). The app only ever pushes flag changes
 * (`read` / `active`); titles/bodies/payloads are server-authored.
 */
@Serializable
data class AppNotification(
    @SerialName("id")
    val uid: String = "",
    @SerialName("type")
    val type: String = "",
    @SerialName("title")
    val title: String = "",
    @SerialName("body")
    val body: String = "",
    @SerialName("data_payload")
    val dataPayload: String? = null,
    @SerialName("read")
    val read: Boolean = false,
    @SerialName("active")
    val active: Boolean = true,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
