package com.ampairs.workspace.sync

import com.ampairs.common.di.AppScope
import com.ampairs.common.event.EventLogger
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Connect/reconnect/hourly bootstrap: fetches the workspace sync checkpoints and hands them to
 * [CentralSyncService.reconcileCheckpoints], which schedules dependency-ordered pulls for any
 * entity whose server `updatedAt` is ahead of the client's last-synced watermark.
 *
 * This is the self-healing path: even if a live WebSocket event was missed, the next bootstrap
 * re-detects the lag from the server checkpoints.
 */
@Inject
@SingleIn(AppScope::class)
class SyncBootstrapService(
    private val checkpointApi: SyncCheckpointApi,
    private val syncService: CentralSyncService,
) {
    suspend fun bootstrap() {
        val response = runCatching { checkpointApi.getCheckpoints() }.getOrElse {
            EventLogger.w("SyncBootstrap", "Checkpoint fetch threw", it)
            return
        }
        val data = response.data
        if (data == null) {
            EventLogger.i("SyncBootstrap", "Checkpoint fetch failed: ${response.error?.message}")
            return
        }
        // Map backend entity codes to SyncEntity, dropping any the client doesn't know about.
        val checkpoints = data.checkpoints.mapNotNull { (code, timestamp) ->
            SyncEntity.fromEntityType(code)?.let { it to timestamp }
        }.toMap()
        EventLogger.i("SyncBootstrap", "Reconciling ${checkpoints.size} entity checkpoint(s)")
        syncService.reconcileCheckpoints(checkpoints)
    }
}
