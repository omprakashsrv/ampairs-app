package com.ampairs.workspace

import com.ampairs.common.di.AppScope
import com.ampairs.common.event.ConnectionState
import com.ampairs.common.event.EventLogger
import com.ampairs.common.event.IEventManager
import com.ampairs.sync.CentralSyncService
import com.ampairs.workspace.sync.SyncBootstrapService
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Manages the WebSocket event connection for the active workspace.
 * All incoming backend events are routed to CentralSyncService — no per-repository
 * event listeners are wired here.
 */
@Inject
@SingleIn(AppScope::class)
class EventConnectionManager(
    private val eventManagerProvider: EventManagerProvider,
    private val syncService: CentralSyncService,
    private val bootstrapService: SyncBootstrapService,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var connectionJob: Job? = null
    private var eventForwardJob: Job? = null
    private var currentEventManager: IEventManager? = null

    fun connectToWorkspace(
        workspaceId: String,
        userId: String,
        deviceId: String,
    ) {
        disconnect()

        connectionJob = scope.launch(Dispatchers.Default) {
            try {
                val eventManager = eventManagerProvider.get(workspaceId, userId, deviceId)
                currentEventManager = eventManager

                EventLogger.i("EventConnectionManager", "Connecting to workspace: $workspaceId")
                eventManager.connect()

                // Forward all backend events to CentralSyncService
                eventForwardJob = launch {
                    eventManager.events.collect { event ->
                        syncService.onBackendEvent(
                            entityType = event.entityType,
                            entityId = event.entityId,
                            eventType = event.eventType.name,
                            lastUpdatedAt = event.lastUpdatedAt,
                        )
                    }
                }

                // On every (re)connect: flush pending pushes AND re-run the checkpoint bootstrap so
                // anything missed while offline is reconciled and pulled in dependency order.
                launch {
                    eventManager.connectionState
                        .filter { it is ConnectionState.Connected }
                        .collect {
                            syncService.onConnectionRestored()
                            bootstrapService.bootstrap()
                        }
                }

                // Hourly safety net — re-reconcile checkpoints in case a live event was missed.
                launch {
                    while (true) {
                        delay(HOURLY_RECONCILE_INTERVAL_MS)
                        if (currentEventManager?.isConnected() == true) {
                            runCatching { bootstrapService.bootstrap() }
                        }
                    }
                }

                EventLogger.i("EventConnectionManager", "✅ Event sync ready for workspace: $workspaceId")
            } catch (e: Exception) {
                EventLogger.e("EventConnectionManager", "Failed to connect to workspace events", e)
            }
        }
    }

    fun disconnect() {
        eventForwardJob?.cancel()
        eventForwardJob = null
        connectionJob?.cancel()
        connectionJob = null

        currentEventManager?.let { manager ->
            scope.launch { manager.disconnect() }
        }
        currentEventManager = null

        EventLogger.i("EventConnectionManager", "Disconnected from workspace events")
    }

    fun isConnected(): Boolean = currentEventManager?.isConnected() ?: false

    private companion object {
        const val HOURLY_RECONCILE_INTERVAL_MS = 60 * 60 * 1000L
    }
}

/**
 * Interface for providing workspace-scoped EventManager instances.
 * Implement this in the DI layer to bridge the event module's infrastructure.
 */
fun interface EventManagerProvider {
    fun get(workspaceId: String, userId: String, deviceId: String): IEventManager
}
