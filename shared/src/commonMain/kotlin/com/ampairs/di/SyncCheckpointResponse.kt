package com.ampairs.di

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `GET /api/event/v1/sync/checkpoints`.
 *
 * @property checkpoints map of backend entity code (matching [com.ampairs.sync.SyncEntity.entityType])
 *           to the server's `max(updatedAt)` for the current workspace, as an ISO-8601 string.
 *           A null value means the entity has no rows — the client must not pull it.
 * @property serverTime authoritative server clock at response time (skew reference / diagnostics).
 */
@Serializable
data class SyncCheckpointResponse(
    @SerialName("checkpoints") val checkpoints: Map<String, String?> = emptyMap(),
    @SerialName("server_time") val serverTime: String? = null,
)
