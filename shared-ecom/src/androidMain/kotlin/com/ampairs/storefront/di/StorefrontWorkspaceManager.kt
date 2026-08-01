package com.ampairs.storefront.di

import com.ampairs.common.di.AppScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.sync.CentralSyncService
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    // Main-dispatched so a deferred DB close is queued AFTER the recomposition that remounts the
    // NavDisplay (on the same main looper) and disposes the old workspace's ViewModelStores.
    private val cleanupScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    fun activate(workspaceId: String, workspaceSlug: String) {
        val old = _session.value

        // Stop sync BEFORE swapping — cancels any in-flight pull/push coroutines that touch the old
        // DB, so they don't resume against a closed connection pool.
        centralSyncService.stop()
        centralSyncService.setDelegates { emptyMap() }

        val config = WorkspaceConfig(workspaceId, workspaceSlug)
        val graph = workspaceGraphFactory.create(config)
        // Install the new session first: the generation bump remounts the NavDisplay and disposes the
        // old workspace's ViewModels (and their Room-Flow collectors) before we close its DB.
        _session.value = Session(++generationCounter, graph, config)

        // Delegates resolved lazily on first sync event — avoids creating every DB upfront.
        centralSyncService.setDelegates { graph.syncDelegates }
        centralSyncService.start(graph.syncStateDao)

        old?.let { closeLater(it) }
    }

    fun clear() {
        val old = _session.value

        centralSyncService.stop()
        centralSyncService.setDelegates { emptyMap() }
        // Null the session first so the UI remounts onto the directory and the old workspace's
        // ViewModels are disposed before its DB is closed.
        _session.value = null

        old?.let { closeLater(it) }
    }

    /**
     * Close the old workspace's registered databases only AFTER the UI has torn down its
     * ViewModels. Closing synchronously here would shut the connection pool while stale Room-`Flow`
     * collectors are still active, throwing `SQLException: Connection pool is closed` into a
     * `viewModelScope` (uncaught → app crash). The close is queued on the main dispatcher (so it runs
     * after the pending recomposition that disposes those ViewModelStores) with a small extra margin.
     */
    private fun closeLater(session: Session) {
        cleanupScope.launch {
            delay(WORKSPACE_CLOSE_DELAY_MS)
            runCatching { session.graph.workspaceResources.close() }
        }
    }

    private companion object {
        const val WORKSPACE_CLOSE_DELAY_MS = 600L
    }
}
