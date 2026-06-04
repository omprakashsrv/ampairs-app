package com.ampairs.common.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class WorkspaceEvent(
    val uid: String,
    // Default + coerceInputValues lets unrecognised event types decode as UNKNOWN instead of failing.
    @SerialName("event_type") val eventType: EventType = EventType.UNKNOWN,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    val payload: JsonObject = JsonObject(emptyMap()),
    // The entity's max(updatedAt) at change time (ISO-8601). Clients compare to their last-synced
    // watermark to decide whether to pull. Null falls back to always-pull.
    @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
    @SerialName("device_id") val deviceId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("sequence_number") val sequenceNumber: Long,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("created_at") val createdAt: String,
) {
    fun isFromDevice(currentDeviceId: String) = deviceId == currentDeviceId
    fun isForEntityType(type: String) = entityType.equals(type, ignoreCase = true)
    fun isForEntity(type: String, id: String) = isForEntityType(type) && entityId == id
}
