package com.ampairs.event

import com.ampairs.common.EventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class WorkspaceEvent(
    val uid: String,
    @SerialName("event_type") val eventType: EventType,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    val payload: JsonObject,
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
