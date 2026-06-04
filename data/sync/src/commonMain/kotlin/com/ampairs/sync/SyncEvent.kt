package com.ampairs.sync

sealed interface SyncEvent {
    /** Pull fresh data from server for a specific entity (or a single record by entityId). */
    data class TriggerPull(val entity: SyncEntity, val entityId: String? = null) : SyncEvent

    /** Push locally unsynced records for an entity to the server. */
    data class TriggerPush(val entity: SyncEntity, val entityId: String? = null) : SyncEvent

    /** Pull + push for all entities (or a single entity if specified). */
    data class TriggerFullSync(val entity: SyncEntity? = null) : SyncEvent

    /** Emitted by EventSyncBridge when a backend WebSocket event arrives. */
    data class BackendEventReceived(
        val entityType: String,
        val entityId: String,
        val eventType: String,
    ) : SyncEvent

    /** Signal the service to reset — emitted on workspace switch before stop()/start(). */
    data object WorkspaceSwitched : SyncEvent
}
