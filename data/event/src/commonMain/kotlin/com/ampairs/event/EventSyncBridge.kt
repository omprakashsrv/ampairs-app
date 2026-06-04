package com.ampairs.event

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncBootstrap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Inject
@SingleIn(WorkspaceScope::class)
class EventSyncBridge(
    private val eventManager: EventManager,
    private val syncService: CentralSyncService,
    private val syncBootstrap: SyncBootstrap,
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
                    lastUpdatedAt = event.lastUpdatedAt,
                )
            }
        }

        // On every (re)connect: flush pending pushes/pulls AND run the checkpoint bootstrap so
        // anything missed while offline is reconciled and pulled in dependency order.
        scope.launch {
            eventManager.connectionState
                .filter { it is ConnectionState.Connected }
                .collect {
                    syncService.onConnectionRestored()
                    syncBootstrap.reconcileOnConnect()
                }
        }

        // Hourly safety net — re-reconcile checkpoints in case a live event was missed.
        scope.launch {
            while (true) {
                delay(HOURLY_RECONCILE_INTERVAL_MS)
                if (eventManager.isConnected()) {
                    runCatching { syncBootstrap.reconcileOnConnect() }
                }
            }
        }
    }

    private companion object {
        const val HOURLY_RECONCILE_INTERVAL_MS = 60 * 60 * 1000L
    }
}
