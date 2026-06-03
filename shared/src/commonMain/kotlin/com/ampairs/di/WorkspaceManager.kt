package com.ampairs.di

import com.ampairs.common.DeviceService
import com.ampairs.common.di.AppScope
import com.ampairs.common.workspace.WorkspaceActivator
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.sync.CentralSyncService
import com.ampairs.workspace.EventConnectionManager
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class WorkspaceManager(
    private val workspaceGraphFactory: WorkspaceGraph.Factory,
    private val centralSyncService: CentralSyncService,
    private val eventConnectionManager: EventConnectionManager,
    private val deviceService: DeviceService,
) : WorkspaceActivator {
    data class WorkspaceSession(
        val generation: Long,
        val graph: WorkspaceGraph,
        val config: WorkspaceConfig,
    )

    private var generationCounter = 0L
    private val _session = MutableStateFlow<WorkspaceSession?>(null)
    val session: StateFlow<WorkspaceSession?> = _session.asStateFlow()

    override fun activateWorkspace(workspaceId: String, workspaceSlug: String, userId: String) {
        val oldSession = _session.value
        oldSession?.graph?.workspaceResources?.close()
        centralSyncService.stop()
        centralSyncService.setDelegates(emptyMap())

        val config = WorkspaceConfig(workspaceId, workspaceSlug)
        val graph = workspaceGraphFactory.create(config)
        val session = WorkspaceSession(++generationCounter, graph, config)
        _session.value = session

        centralSyncService.setDelegates(graph.syncDelegates)
        centralSyncService.start(graph.syncStateDatabase)

        val deviceId = deviceService.getDeviceId()
        eventConnectionManager.connectToWorkspace(
            workspaceId = workspaceId,
            userId = userId,
            deviceId = deviceId,
        )
    }

    fun clearSession() {
        _session.value?.graph?.workspaceResources?.close()
        centralSyncService.stop()
        centralSyncService.setDelegates(emptyMap())
        _session.value = null
    }
}
