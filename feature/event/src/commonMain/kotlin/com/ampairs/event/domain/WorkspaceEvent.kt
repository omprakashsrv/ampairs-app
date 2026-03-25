package com.ampairs.event.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Workspace event that gets broadcasted to all connected devices.
 * Represents a domain entity change (create, update, delete) that occurred on another device.
 *
 * Matches backend WorkspaceEventResponse DTO.
 */
@Serializable
data class WorkspaceEvent(
    val uid: String,
    @SerialName("event_type")
    val eventType: EventType,
    @SerialName("entity_type")
    val entityType: String, // "customer", "product", "order", "invoice"
    @SerialName("entity_id")
    val entityId: String, // UID of the affected entity
    val payload: JsonObject, // Dynamic JSON payload with change details
    @SerialName("device_id")
    val deviceId: String, // Originating device ID (to filter own events)
    @SerialName("user_id")
    val userId: String, // User who triggered the event
    @SerialName("sequence_number")
    val sequenceNumber: Long, // For ordering and catch-up
    @SerialName("workspace_id")
    val workspaceId: String, // Workspace context
    @SerialName("created_at")
    val createdAt: String // ISO 8601 timestamp
) {
    /**
     * Check if this event originated from the current device
     */
    fun isFromDevice(currentDeviceId: String): Boolean {
        return deviceId == currentDeviceId
    }

    /**
     * Check if this event is for the given entity type
     */
    fun isForEntityType(type: String): Boolean {
        return entityType.equals(type, ignoreCase = true)
    }

    /**
     * Check if this event is for a specific entity
     */
    fun isForEntity(type: String, id: String): Boolean {
        return isForEntityType(type) && entityId == id
    }
}
