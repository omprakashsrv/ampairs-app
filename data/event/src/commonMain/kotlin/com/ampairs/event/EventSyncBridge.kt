package com.ampairs.event

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.sync.CentralSyncService
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Inject
@SingleIn(WorkspaceScope::class)
class EventSyncBridge(
    private val eventManager: EventManager,
    private val syncService: CentralSyncService,
    closableRegistry: WorkspaceClosableRegistry,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        closableRegistry.register { scope.cancel() }
    }

    fun start() {
        scope.launch { eventManager.connect() }

        scope.launch {
            eventManager.events.collect { event ->
                syncService.onBackendEvent(
                    entityType = event.entityType,
                    entityId = event.entityId,
                    eventType = event.eventType.name,
                )
            }
        }

        scope.launch {
            eventManager.connectionState
                .filter { it is ConnectionState.Connected }
                .collect { syncService.onConnectionRestored() }
        }
    }
}
