package com.ampairs.di

import co.touchlab.kermit.Logger
import com.ampairs.common.di.AppScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncBootstrap
import com.ampairs.sync.SyncEntity
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

private val log = Logger.withTag("SyncBootstrap")

/**
 * Default [SyncBootstrap]: fetches the workspace sync checkpoints and hands them to
 * [CentralSyncService.reconcileCheckpoints], which schedules dependency-ordered pulls for any
 * entity whose server `updatedAt` is ahead of what was last pulled this session.
 *
 * This is the self-healing path: even if a live WebSocket event was missed, the next bootstrap
 * (connect / reconnect / hourly) re-detects the lag from the server checkpoints.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultSyncBootstrap(
    private val checkpointApi: SyncCheckpointApi,
    private val syncService: CentralSyncService,
) : SyncBootstrap {

    override suspend fun reconcileOnConnect() {
        val response = runCatching { checkpointApi.getCheckpoints() }.getOrElse {
            log.w(it) { "Checkpoint fetch threw" }
            return
        }
        val data = response.data
        if (data == null) {
            log.i { "Checkpoint fetch failed: ${response.error?.message}" }
            return
        }
        // Map backend entity codes to SyncEntity, dropping any the client doesn't know about.
        val checkpoints = data.checkpoints.mapNotNull { (code, timestamp) ->
            SyncEntity.fromEntityType(code)?.let { it to timestamp }
        }.toMap()
        log.i { "Reconciling ${checkpoints.size} entity checkpoint(s)" }
        syncService.reconcileCheckpoints(checkpoints)
    }
}
