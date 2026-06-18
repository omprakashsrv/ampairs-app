package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.connector.data.api.ConnectorApi
import com.ampairs.connector.domain.ConnectorConfigProvider
import com.ampairs.connector.domain.SyncRunDto
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val log = Logger.withTag("TallySyncScheduler")

@Inject
@SingleIn(WorkspaceScope::class)
class TallySyncScheduler(
    val syncService: TallySyncService,
    val centralSyncService: CentralSyncService,
    private val connectorConfigProvider: ConnectorConfigProvider,
    private val connectorApi: ConnectorApi,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    var lastResult: TallySyncResult? = null
        private set

    fun start(workspaceSlug: String, interval: Duration = 15.minutes) {
        if (job?.isActive == true) return
        log.i { "Starting Tally sync scheduler (interval=${interval})" }
        // Hydrate the offline connector mirror (installation list) via the ConnectorSyncDelegate.
        centralSyncService.emit(SyncEvent.TriggerPull(SyncEntity.CONNECTOR))
        job = scope.launch {
            while (isActive) {
                // Respect a backend-paused connector (FR-023/FR-025): skip the scheduled cycle while
                // PAUSED. Manual "Sync Now" (runOnce) still works regardless.
                val paused = runCatching { connectorConfigProvider.installation()?.status }
                    .getOrNull()?.equals("PAUSED", ignoreCase = true) == true
                if (paused) log.i { "Tally connector PAUSED — skipping scheduled cycle" }
                else runOnce(workspaceSlug)
                delay(interval)
            }
        }
    }

    /** Runs a single Tally sync cycle and marks affected entities as pending push. */
    suspend fun runOnce(workspaceSlug: String): TallySyncResult {
        log.d { "Tally sync triggered for workspace=$workspaceSlug" }
        val result = runCatching { syncService.sync(workspaceSlug) }
            .onFailure { log.e(it) { "Tally sync error" } }
            .getOrElse { TallySyncResult(error = it.message) }
        lastResult = result

        // When the rows were pushed to the backend connector (sparse upsert, non-destructive), do NOT
        // also markPendingPush — that would re-push the full entity via the legacy /sync path and
        // re-introduce the data loss the connector push avoids.
        if (result.success && !result.pushedViaConnector) {
            if (result.customerGroupsSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.CUSTOMER_GROUP)
            if (result.customersSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.CUSTOMER)
            if (result.productsSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.PRODUCT)
            if (result.groupsSynced > 0 || result.categoriesSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.PRODUCT_CATALOG)
        }

        // Report the cycle to the backend connector platform (run history). Non-fatal.
        // Host/port-from-config (FR-H03) and the mapped sparse-upsert push both live in
        // TallySyncService.sync() now; remaining follow-up is advancing per-entity checkpoints.
        runCatching { reportRunToConnector(result) }
            .onFailure { log.w(it) { "Connector run report failed (non-fatal)" } }

        return result
    }

    /**
     * Backend connector status for the Tally settings screen — whether Tally is installed on the
     * platform and, if so, the connection details the backend config supplies (FR-H03). All lookups
     * are best-effort: a backend error yields [ConnectorStatus.installed] = false rather than throwing.
     */
    suspend fun connectorStatus(): ConnectorStatus {
        val installation = runCatching { connectorConfigProvider.installation() }.getOrNull()
            ?: return ConnectorStatus(installed = false)
        val config = runCatching { connectorConfigProvider.config(installation.uid) }.getOrNull()
        return ConnectorStatus(
            installed = true,
            connectorType = installation.connectorType,
            status = installation.status,
            host = config?.nonSecretValues?.get("host")?.takeIf { it.isNotBlank() },
            port = config?.nonSecretValues?.get("port")?.trim()?.toIntOrNull(),
            lastValidatedAt = config?.lastValidatedAt,
            lastErrorMessage = installation.lastErrorMessage,
        )
    }

    /** Records this Tally cycle as a connector sync-run on the backend, if Tally is installed there. */
    private suspend fun reportRunToConnector(result: TallySyncResult) {
        val installation = connectorConfigProvider.installation() ?: return // not installed on backend
        val total = result.customersSynced + result.customerGroupsSynced + result.productsSynced +
            result.groupsSynced + result.categoriesSynced
        connectorApi.recordRun(
            installation.uid,
            SyncRunDto(
                installationUid = installation.uid,
                trigger = "SCHEDULED",
                status = if (result.success) "SUCCESS" else "FAILED",
                processed = total,
                updated = total,
                errorDetail = result.error,
            ),
        )
    }

    /** Pause the backend connector (FR-025); the scheduled loop then skips cycles until resumed. */
    suspend fun pauseConnector(): Boolean = setPaused(pause = true)

    /** Resume the backend connector so scheduled cycles run again. */
    suspend fun resumeConnector(): Boolean = setPaused(pause = false)

    private suspend fun setPaused(pause: Boolean): Boolean {
        val uid = runCatching { connectorConfigProvider.installation()?.uid }.getOrNull() ?: return false
        return runCatching {
            if (pause) connectorApi.pause(uid) else connectorApi.resume(uid)
            true
        }.getOrElse {
            log.w(it) { "Connector ${if (pause) "pause" else "resume"} failed" }
            false
        }
    }

    fun stop() {
        log.i { "Stopping Tally sync scheduler" }
        job?.cancel()
        job = null
    }

    fun cancel() {
        scope.cancel()
    }
}

/** Snapshot of the backend Tally connector for display in the desktop settings screen. */
data class ConnectorStatus(
    val installed: Boolean,
    val connectorType: String? = null,
    val status: String? = null,
    val host: String? = null,
    val port: Int? = null,
    val lastValidatedAt: String? = null,
    val lastErrorMessage: String? = null,
)
