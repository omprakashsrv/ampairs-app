package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
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
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    var lastResult: TallySyncResult? = null
        private set

    fun start(workspaceSlug: String, interval: Duration = 15.minutes) {
        if (job?.isActive == true) return
        log.i { "Starting Tally sync scheduler (interval=${interval})" }
        job = scope.launch {
            while (isActive) {
                runOnce(workspaceSlug)
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

        if (result.success) {
            if (result.customerGroupsSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.CUSTOMER_GROUP)
            if (result.customersSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.CUSTOMER)
            if (result.productsSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.PRODUCT)
            if (result.groupsSynced > 0 || result.categoriesSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.PRODUCT_CATALOG)
            if (result.invoicesSynced > 0)
                centralSyncService.markPendingPush(SyncEntity.INVOICE)
            // Payments: the repository + ledger poster already flag these inside save()/postDocumentEntry;
            // marking again is harmless and keeps the scheduler self-documenting.
            if (result.paymentsSynced > 0) {
                centralSyncService.markPendingPush(SyncEntity.PAYMENT_VOUCHER)
                centralSyncService.markPendingPush(SyncEntity.PAYMENT_ALLOCATION)
            }
            // Invoice + payment postings both write ledger entries and recompute party balances.
            if (result.invoicesSynced > 0 || result.paymentsSynced > 0) {
                centralSyncService.markPendingPush(SyncEntity.LEDGER_ENTRY)
                centralSyncService.markPendingPush(SyncEntity.PARTY_BALANCE)
            }
        }

        return result
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
