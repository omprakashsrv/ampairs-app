package com.ampairs.workspace

import com.ampairs.common.di.AppScope
import com.ampairs.common.event.EventLogger
import com.ampairs.common.event.IEventManager
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Helper class to manage EventManager connection and repository event listeners.
 * Simplifies workspace selection by centralizing event setup.
 */
@Inject
@SingleIn(AppScope::class)
class EventConnectionManager(
    private val eventManagerProvider: EventManagerProvider,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var connectionJob: Job? = null
    private var currentEventManager: IEventManager? = null

    /**
     * Connect to workspace events and setup all repository listeners.
     *
     * @param workspaceId Workspace UID
     * @param userId Current user UID
     * @param deviceId Current device UID
     */
    fun connectToWorkspace(
        workspaceId: String,
        userId: String,
        deviceId: String,
        scope: CoroutineScope
    ) {
        // Disconnect from previous workspace if any
        disconnect()

        connectionJob = scope.launch(Dispatchers.Default) {
            try {
                // 1. Get EventManager for this workspace
                val eventManager = eventManagerProvider.get(workspaceId, userId, deviceId)
                currentEventManager = eventManager

                // 2. Connect to WebSocket
                EventLogger.i("EventConnectionManager", "Connecting to workspace: $workspaceId")
                eventManager.connect()

                // 3. Setup repository event listeners
                setupRepositoryListeners(eventManager)

                EventLogger.i("EventConnectionManager", "✅ Event sync ready for workspace: $workspaceId")

            } catch (e: Exception) {
                EventLogger.e("EventConnectionManager", "Failed to connect to workspace events", e)
            }
        }
    }

    /**
     * Setup event listeners for all repositories that need real-time sync.
     * Repositories register their own listeners separately via DI-provided callbacks.
     */
    private fun setupRepositoryListeners(eventManager: IEventManager) {
        EventLogger.i("EventConnectionManager", "Repository event listeners configured")
    }

    /**
     * Disconnect from workspace events and cleanup.
     */
    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null

        currentEventManager?.let { manager ->
            scope.launch {
                manager.disconnect()
            }
        }
        currentEventManager = null

        EventLogger.i("EventConnectionManager", "Disconnected from workspace events")
    }

    /**
     * Get current connection state.
     */
    fun isConnected(): Boolean {
        return currentEventManager?.isConnected() ?: false
    }
}

/**
 * Interface for providing workspace-scoped EventManager instances.
 * Implement this in the DI layer to bridge the event module's infrastructure.
 */
fun interface EventManagerProvider {
    fun get(workspaceId: String, userId: String, deviceId: String): IEventManager
}
