package com.ampairs.event

import com.ampairs.event.util.EventLogger
import io.ktor.client.HttpClient

/**
 * Factory for creating and managing workspace-scoped EventManager instances.
 * Ensures one EventManager per workspace with proper lifecycle management.
 *
 * Usage:
 * ```kotlin
 * // Create or get existing EventManager for workspace
 * val eventManager = EventManagerFactory.getOrCreate(
 *     workspaceId = "workspace-uid",
 *     userId = "user-uid",
 *     deviceId = "device-uid",
 *     httpClient = httpClient,
 *     tokenProvider = { authRepository.getAccessToken() },
 *     baseUrl = "http://api.ampairs.com"
 * )
 *
 * // Connect
 * eventManager.connect()
 *
 * // When switching workspaces
 * EventManagerFactory.clearWorkspace("old-workspace-uid")
 * ```
 */
object EventManagerFactory {
    private val managers = mutableMapOf<String, EventManager>()

    /**
     * Get existing EventManager for workspace or create new one.
     * Returns the same instance for the same workspaceId.
     *
     * @param workspaceId Workspace identifier
     * @param userId Current user identifier
     * @param deviceId Current device identifier
     * @param httpClient Ktor HTTP client for WebSocket transport
     * @param tokenProvider Function to get current JWT access token
     * @param tokenRefresher Function to refresh expired tokens
     * @param baseUrl API base URL
     * @return EventManager instance for the workspace
     */
    fun getOrCreate(
        workspaceId: String,
        userId: String,
        deviceId: String,
        httpClient: HttpClient,
        tokenProvider: suspend () -> String,
        tokenRefresher: suspend () -> Boolean,
        baseUrl: String
    ): EventManager {
        return managers.getOrPut(workspaceId) {
            EventLogger.i("EventManagerFactory", "Creating EventManager for workspace: $workspaceId")
            EventManager(
                workspaceId = workspaceId,
                userId = userId,
                deviceId = deviceId,
                httpClient = httpClient,
                tokenProvider = tokenProvider,
                tokenRefresher = tokenRefresher,
                baseUrl = baseUrl
            )
        }
    }

    /**
     * Get existing EventManager for workspace without creating new one.
     * Returns null if no EventManager exists for the workspace.
     *
     * @param workspaceId Workspace identifier
     * @return EventManager instance or null
     */
    fun get(workspaceId: String): EventManager? {
        return managers[workspaceId]
    }

    /**
     * Clear EventManager for a specific workspace.
     * Disconnects and removes the EventManager instance.
     *
     * @param workspaceId Workspace identifier
     */
    suspend fun clearWorkspace(workspaceId: String) {
        managers[workspaceId]?.let { manager ->
            EventLogger.i("EventManagerFactory", "Clearing EventManager for workspace: $workspaceId")
            manager.disconnect()
            managers.remove(workspaceId)
        }
    }

    /**
     * Clear all EventManagers.
     * Disconnects and removes all instances.
     * Useful when logging out or app shutdown.
     */
    suspend fun clearAll() {
        EventLogger.i("EventManagerFactory", "Clearing all EventManagers (${managers.size} instances)")
        managers.values.forEach { it.disconnect() }
        managers.clear()
    }

    /**
     * Get number of active EventManager instances
     */
    fun getActiveCount(): Int {
        return managers.size
    }

    /**
     * Get list of workspace IDs with active EventManagers
     */
    fun getActiveWorkspaces(): List<String> {
        return managers.keys.toList()
    }
}
