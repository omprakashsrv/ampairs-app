package com.ampairs.event

import com.ampairs.common.EventType
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IEventManager {
    val events: SharedFlow<WorkspaceEvent>
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect()
    suspend fun disconnect()
    fun isConnected(): Boolean
    fun getEventsForEntityType(entityType: String): SharedFlow<WorkspaceEvent>
    fun getEventsByType(vararg eventTypes: EventType): SharedFlow<WorkspaceEvent>
}
