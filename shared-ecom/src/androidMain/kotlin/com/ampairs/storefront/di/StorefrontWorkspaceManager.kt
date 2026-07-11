package com.ampairs.storefront.di

import com.ampairs.common.di.AppScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.sync.CentralSyncService
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Creates and holds the single [StorefrontWorkspaceGraph] for the pinned "storefront-enterprise" workspace.
 * Unlike the full app there is no workspace picker — [activate] is called once at startup.
 *
 * Mirrors :shared `WorkspaceManager`: closing the old graph's registered DBs, restarting
 * [CentralSyncService] with the new graph's delegates (resolved lazily so DBs aren't all created
 * eagerly). The `generation` counter forces the NavDisplay + its ViewModelStores to remount when a
 * new session is installed.
 */
@Inject
@SingleIn(AppScope::class)
class StorefrontWorkspaceManager(
    private val workspaceGraphFactory: StorefrontWorkspaceGraph.Factory,
    private val centralSyncService: CentralSyncService,
) {
    data class Session(
        val generation: Long,
        val graph: StorefrontWorkspaceGraph,
        val config: WorkspaceConfig,
    )

    private var generationCounter = 0L
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    fun activate(workspaceId: String, workspaceSlug: String) {
        _session.value?.graph?.workspaceResources?.close()
        centralSyncService.stop()
        centralSyncService.setDelegates { emptyMap() }

        val config = WorkspaceConfig(workspaceId, workspaceSlug)
        val graph = workspaceGraphFactory.create(config)
        _session.value = Session(++generationCounter, graph, config)

        // Delegates resolved lazily on first sync event — avoids creating every DB upfront.
        centralSyncService.setDelegates { graph.syncDelegates }
        centralSyncService.start(graph.syncStateDao)
    }

    fun clear() {
        _session.value?.graph?.workspaceResources?.close()
        centralSyncService.stop()
        centralSyncService.setDelegates { emptyMap() }
        _session.value = null
    }
}
