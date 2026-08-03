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

        // Per-entity legacy-push gating. The connector sparse-upsert only covers a SUBSET of the
        // entities Tally writes (customer, customer_group, product, product_catalog, unit,
        // stock_balance). For those, the legacy full `/sync` push must be skipped when the connector
        // took them — a full push nulls unmapped columns (the data loss the connector avoids). For
        // every OTHER entity Tally writes there is no connector writer, so they MUST still push via
        // the legacy path even when a connector is installed, or they'd never reach the backend.
        if (result.success) {
            // --- Connector-covered entities: suppress legacy push only when pushed via connector ---
            if (!result.pushedViaConnector) {
                if (result.customerGroupsSynced > 0)
                    centralSyncService.markPendingPush(SyncEntity.CUSTOMER_GROUP)
                if (result.customersSynced > 0)
                    centralSyncService.markPendingPush(SyncEntity.CUSTOMER)
                if (result.productsSynced > 0)
                    centralSyncService.markPendingPush(SyncEntity.PRODUCT)
                if (result.groupsSynced > 0 || result.categoriesSynced > 0)
                    centralSyncService.markPendingPush(SyncEntity.PRODUCT_CATALOG)
                // Unit conversions ride the product /sync feed (ProductSyncDelegate attaches them on
                // push). The connector product push does NOT carry conversions, so under a connector
                // they currently don't reach the backend — flagging PRODUCT here would re-push the
                // full product and clobber connector-managed columns. Known limitation: a dedicated
                // conversion connector writer is a follow-up.
                if (result.unitConversionsSynced > 0)
                    centralSyncService.markPendingPush(SyncEntity.PRODUCT)
            }

            // --- Entities with NO connector writer (or whose connector write is only a subset):
            //     ALWAYS push via legacy /sync so they reach the backend even under a connector
            //     (separate tables → no unmapped-column data loss). INVENTORY note: the connector
            //     stock_balance path now applies via a ledger-consistent physical COUNT, but it only
            //     reconciles ALREADY-TRACKED items (opt-in, FR-004) — so the legacy inventory push
            //     still carries the full local inventory state. The COUNT's idempotency guard (skip
            //     when unchanged) neutralizes the overlap; converging both paths is a follow-up. ---
            if (result.suppliersSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.SUPPLIER)
            if (result.pricesSynced > 0) {
                centralSyncService.markPendingPush(SyncEntity.PRICE_LIST)
                centralSyncService.markPendingPush(SyncEntity.PRICE_LIST_ITEM)
            }
            if (result.standardCostsSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.PRODUCT_STANDARD_COST)
            // Stock balances → inventory items + opening transactions.
            if (result.inventoryItemsSynced > 0) {
                centralSyncService.markPendingPush(SyncEntity.INVENTORY)
                centralSyncService.markPendingPush(SyncEntity.INVENTORY_TRANSACTION)
            }
            if (result.invoicesSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.INVOICE)
            if (result.purchasesSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.PURCHASE)
            // Payments: the repository + ledger poster already flag these inside save()/postDocumentEntry;
            // marking again is harmless and keeps the scheduler self-documenting.
            if (result.paymentsSynced > 0) {
                centralSyncService.markPendingPush(SyncEntity.PAYMENT_VOUCHER)
                centralSyncService.markPendingPush(SyncEntity.PAYMENT_ALLOCATION)
            }
            // Invoice + purchase + payment postings all write ledger entries and recompute party
            // balances (purchases via the buy-side PURCHASE_BILL → supplier "To Pay").
            if (result.invoicesSynced > 0 || result.purchasesSynced > 0 || result.paymentsSynced > 0) {
                centralSyncService.markPendingPush(SyncEntity.LEDGER_ENTRY)
                centralSyncService.markPendingPush(SyncEntity.PARTY_BALANCE)
            }
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
            // The Ktor helpers return backend failures in Response.error (they don't throw), so a
            // rejected pause/resume must be detected here — otherwise the UI reports success on a 4xx.
            val response = if (pause) connectorApi.pause(uid) else connectorApi.resume(uid)
            val ok = response.data != null && response.error == null
            if (!ok) log.w { "Connector ${if (pause) "pause" else "resume"} rejected: ${response.error?.message}" }
            ok
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
