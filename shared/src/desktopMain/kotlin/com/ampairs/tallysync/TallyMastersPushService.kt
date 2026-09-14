package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.tally.TallyApiImpl
import com.ampairs.tally.TallyRepository
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.first

/** Outcome of one master type's push (units, stock groups, stock categories, account groups, customers, suppliers, stock items). */
data class TallyMasterPushResult(
    val pushed: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
) {
    val hasFailures get() = failed > 0
}

/** Pending (not-yet-pushed) count per master type — drives the per-type rows on `TallySettingsScreen`. */
data class TallyMasterCounts(
    val units: Int = 0,
    val stockGroups: Int = 0,
    val stockCategories: Int = 0,
    val accountGroups: Int = 0,
    val customers: Int = 0,
    val suppliers: Int = 0,
    val stockItems: Int = 0,
)

/** Aggregated outcome of a full [TallyMastersPushService.push] cycle, one result per master type. */
data class TallyMastersPushResult(
    val units: TallyMasterPushResult = TallyMasterPushResult(),
    val stockGroups: TallyMasterPushResult = TallyMasterPushResult(),
    val stockCategories: TallyMasterPushResult = TallyMasterPushResult(),
    val accountGroups: TallyMasterPushResult = TallyMasterPushResult(),
    val customers: TallyMasterPushResult = TallyMasterPushResult(),
    val suppliers: TallyMasterPushResult = TallyMasterPushResult(),
    val stockItems: TallyMasterPushResult = TallyMasterPushResult(),
    val error: String? = null,
) {
    val success get() = error == null
    val totalPushed get() = units.pushed + stockGroups.pushed + stockCategories.pushed +
        accountGroups.pushed + customers.pushed + suppliers.pushed + stockItems.pushed
    val totalFailed get() = units.failed + stockGroups.failed + stockCategories.failed +
        accountGroups.failed + customers.failed + suppliers.failed + stockItems.failed
}

private val kermitLog = Logger.withTag("TallyMastersPush")

/**
 * Pushes locally-created Customer/Supplier/Product masters (and the Unit/StockGroup/StockCategory/
 * AccountGroup masters they reference) *into* Tally, so app-created entities get a real Tally
 * counterpart before [TallyInvoicePushService] pushes an invoice that references them — closing the
 * gap documented in [TallyInvoiceVoucherMapper] ("the party ledger and every stock item must already
 * exist in Tally"). [push] (the bulk sweep) runs unconditionally before every invoice push (see
 * [TallySyncScheduler.pushInvoices]) — cheap to no-op since candidates are filtered by blank `ref_id`.
 * The individual `pushXxx` methods below are the manual, per-type push controls on
 * `TallySettingsScreen`, for pushing (and verifying in Tally) one level at a time.
 *
 * Dependency order — a master referencing a parent name that doesn't exist yet is rejected by Tally:
 * ```
 * Unit ─┐
 *       ├─→ StockItem (Product)
 * StockGroup / StockCategory ┘
 * AccountGroup (custom sub-groups) ─→ Customer / Supplier (Ledger)
 * ```
 * Within one level, individual failures don't block siblings (best-effort, matching
 * [TallyInvoicePushService]). Across levels, a dependent record whose specific parent isn't yet in
 * Tally is skipped per-record (not the whole level) — see [TallyLedgerPushService]/
 * [TallyStockItemPushService].
 */
@Inject
class TallyMastersPushService(
    private val engine: HttpClientEngine,
    private val unitPushService: TallyUnitPushService,
    private val stockGroupPushService: TallyStockGroupPushService,
    private val stockCategoryPushService: TallyStockCategoryPushService,
    private val accountGroupPushService: TallyAccountGroupPushService,
    private val ledgerPushService: TallyLedgerPushService,
    private val stockItemPushService: TallyStockItemPushService,
    private val dataStore: AppPreferencesDataStore,
) {
    private suspend fun buildRepo(workspaceSlug: String, log: (String) -> Unit): TallyRepository? {
        val host = dataStore.getTallyHost(workspaceSlug).first()
        if (host.isBlank()) {
            log("Tally masters push: host not configured")
            return null
        }
        val port = dataStore.getTallyPort(workspaceSlug).first()
        return TallyRepository(TallyApiImpl(engine, "http://$host:$port"))
    }

    /** Pending count per master type — pure local reads, safe to call freely (screen load, post-push refresh). */
    suspend fun pendingCounts(workspaceSlug: String): TallyMasterCounts = TallyMasterCounts(
        units = unitPushService.pendingCount(workspaceSlug),
        stockGroups = stockGroupPushService.pendingCount(workspaceSlug),
        stockCategories = stockCategoryPushService.pendingCount(workspaceSlug),
        accountGroups = accountGroupPushService.pendingCount(workspaceSlug),
        customers = ledgerPushService.pendingCustomerCount(workspaceSlug),
        suppliers = ledgerPushService.pendingSupplierCount(workspaceSlug),
        stockItems = stockItemPushService.pendingCount(workspaceSlug),
    )

    /** Bulk sweep — all seven master types in dependency order, one shared connection. */
    suspend fun push(workspaceSlug: String, log: (String) -> Unit): TallyMastersPushResult {
        val repo = buildRepo(workspaceSlug, log)
            ?: return TallyMastersPushResult(error = "Tally host not configured")

        return runCatching {
            val units = unitPushService.push(workspaceSlug, repo, log)
            val stockGroups = stockGroupPushService.push(workspaceSlug, repo, log)
            val stockCategories = stockCategoryPushService.push(workspaceSlug, repo, log)
            val accountGroups = accountGroupPushService.push(workspaceSlug, repo, log)
            val customers = ledgerPushService.pushCustomers(workspaceSlug, repo, log)
            val suppliers = ledgerPushService.pushSuppliers(workspaceSlug, repo, log)
            val stockItems = stockItemPushService.push(workspaceSlug, repo, log)
            TallyMastersPushResult(
                units = units,
                stockGroups = stockGroups,
                stockCategories = stockCategories,
                accountGroups = accountGroups,
                customers = customers,
                suppliers = suppliers,
                stockItems = stockItems,
            )
        }.onFailure { kermitLog.e(it) { "Tally masters push error" } }
            .getOrElse { TallyMastersPushResult(error = it.message) }
    }

    // --- Manual, per-type push controls (TallySettingsScreen) --------------------------------

    suspend fun pushUnits(workspaceSlug: String, log: (String) -> Unit): TallyMasterPushResult {
        val repo = buildRepo(workspaceSlug, log) ?: return TallyMasterPushResult()
        return unitPushService.push(workspaceSlug, repo, log)
    }

    suspend fun pushStockGroups(workspaceSlug: String, log: (String) -> Unit): TallyMasterPushResult {
        val repo = buildRepo(workspaceSlug, log) ?: return TallyMasterPushResult()
        return stockGroupPushService.push(workspaceSlug, repo, log)
    }

    suspend fun pushStockCategories(workspaceSlug: String, log: (String) -> Unit): TallyMasterPushResult {
        val repo = buildRepo(workspaceSlug, log) ?: return TallyMasterPushResult()
        return stockCategoryPushService.push(workspaceSlug, repo, log)
    }

    suspend fun pushAccountGroups(workspaceSlug: String, log: (String) -> Unit): TallyMasterPushResult {
        val repo = buildRepo(workspaceSlug, log) ?: return TallyMasterPushResult()
        return accountGroupPushService.push(workspaceSlug, repo, log)
    }

    suspend fun pushCustomers(workspaceSlug: String, log: (String) -> Unit): TallyMasterPushResult {
        val repo = buildRepo(workspaceSlug, log) ?: return TallyMasterPushResult()
        return ledgerPushService.pushCustomers(workspaceSlug, repo, log)
    }

    suspend fun pushSuppliers(workspaceSlug: String, log: (String) -> Unit): TallyMasterPushResult {
        val repo = buildRepo(workspaceSlug, log) ?: return TallyMasterPushResult()
        return ledgerPushService.pushSuppliers(workspaceSlug, repo, log)
    }

    suspend fun pushStockItems(workspaceSlug: String, log: (String) -> Unit): TallyMasterPushResult {
        val repo = buildRepo(workspaceSlug, log) ?: return TallyMasterPushResult()
        return stockItemPushService.push(workspaceSlug, repo, log)
    }
}
