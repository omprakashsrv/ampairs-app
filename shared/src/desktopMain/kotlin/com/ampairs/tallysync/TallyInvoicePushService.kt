package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.invoice.db.model.TaxInfoEntity
import com.ampairs.tally.TallyApiImpl
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.buildVoucherImport
import com.ampairs.tally.renderTallyXml
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/** Outcome of a Tally invoice-push cycle. */
data class TallyPushResult(
    val pushed: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val error: String? = null,
) {
    val success get() = error == null
}

private val kermitLog = Logger.withTag("TallyInvoicePush")

/**
 * Pushes locally-created invoices (order → invoice, spec 010) *into* Tally as Sales vouchers — the
 * write direction that complements [TallySyncService]'s pull. Offline-first friendly: it only reads
 * Room + posts to Tally, and records which invoice ids have been pushed in the DataStore so a second
 * run is idempotent (no duplicate vouchers in Tally).
 *
 * Scope (v1): invoices originated in the app — id not prefixed `INVTLY` (those came *from* Tally),
 * active, numbered, with a named customer, and not already pushed. Each is sent as its own IMPORTDATA
 * request so a single bad voucher (missing ledger/stock item) fails just that invoice; the rest still
 * land. After a push the caller should trigger a normal Tally→local sync so the created voucher is
 * reconciled back (matched by REMOTEID — see [TallyVoucherMapper]).
 */
@Inject
class TallyInvoicePushService(
    private val engine: HttpClientEngine,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val unitDao: UnitDao,
    private val dataStore: AppPreferencesDataStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Local id prefixes that identify a Tally-originated invoice (must never be pushed back). */
    private val tallyOriginatedPrefixes = listOf("INVTLY")

    /**
     * Pushes all not-yet-pushed local invoices. [log] streams human-readable progress to the shared
     * Tally sync log panel. Returns counts; individual failures are logged, not thrown.
     */
    suspend fun push(workspaceSlug: String, log: (String) -> Unit): TallyPushResult {
        val host = dataStore.getTallyHost(workspaceSlug).first()
        if (host.isBlank()) {
            log("Tally host not configured")
            return TallyPushResult(error = "Tally host not configured")
        }
        val port = dataStore.getTallyPort(workspaceSlug).first()
        val baseUrl = "http://$host:$port"
        val repo = TallyRepository(TallyApiImpl(engine, baseUrl))

        val pushedIds = dataStore.getTallyPushedInvoiceIds(workspaceSlug).first()
        val unitNameById = unitDao.getAllUnits().first()
            .associate { it.id to it.shortName.ifBlank { it.name } }

        val candidates = invoiceDao.selectAll().filter { inv ->
            inv.soft_deleted == 0L &&
                inv.invoice_number.isNotBlank() &&
                inv.customer_name.isNotBlank() &&
                inv.id !in pushedIds &&
                tallyOriginatedPrefixes.none { inv.id.startsWith(it) }
        }
        if (candidates.isEmpty()) {
            log("Tally push: no new invoices to push")
            return TallyPushResult()
        }
        log("Tally push: ${candidates.size} invoice(s) to push — $baseUrl")

        var pushed = 0
        var failed = 0
        val newlyPushed = mutableSetOf<String>()
        for (inv in candidates) {
            val items = invoiceItemDao.getInvoiceItems(inv.id)
            val taxComponents: List<TaxInfoEntity> = inv.tax_info
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.decodeFromString<List<TaxInfoEntity>>(it) }.getOrNull() }
                ?: emptyList()

            val voucher = TallyInvoiceVoucherMapper.buildSalesVoucher(
                entity = inv,
                items = items,
                taxComponents = taxComponents,
                unitNameById = unitNameById,
            )

            // Surface the exact request XML in the log (Copy button on the log panel) so the first
            // live iteration against Tally can be diagnosed without a network trace.
            log("  → request ${inv.invoice_number}: ${renderTallyXml(buildVoucherImport(listOf(voucher)))}")

            val outcome = runCatching { repo.importVouchers(listOf(voucher)) }
            val response = outcome.getOrNull()
            val result = response?.body?.data?.importResult
            val topStatus = response?.header?.status?.trim()

            val ok = when {
                outcome.isFailure -> false
                result != null -> result.isSuccess
                else -> topStatus == "1"
            }
            if (ok) {
                pushed++
                newlyPushed += inv.id
                log("  ✓ ${inv.invoice_number} → Tally")
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally rejected the voucher (status=$topStatus)"
                log("  ✗ ${inv.invoice_number} — $reason")
                kermitLog.w { "Tally push failed for ${inv.id}: $reason" }
            }
        }

        if (newlyPushed.isNotEmpty()) dataStore.addTallyPushedInvoiceIds(workspaceSlug, newlyPushed)
        log("Tally push complete — pushed=$pushed, failed=$failed")
        return TallyPushResult(pushed = pushed, failed = failed)
    }
}
