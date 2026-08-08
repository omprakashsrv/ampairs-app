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
 * Room + posts to Tally.
 *
 * **Idempotency** is anchored on the invoice's own `ref_id`: after Tally accepts a voucher we capture
 * its master id (`LASTMID`, falling back to `LASTVCHID`) into `InvoiceEntity.ref_id` via
 * [InvoiceDao.setTallyRef], and skip any invoice that already carries a non-blank `ref_id`. Because
 * `ref_id` round-trips through the invoice `/sync` contract it survives reinstall, and the voucher's
 * `REMOTEID = local invoice id` is the second guard at the Tally end (a re-import matches the existing
 * voucher instead of duplicating). The DataStore pushed-ids set is still written so the Tally→local
 * reconciliation can skip vouchers we authored (see [TallySyncService]).
 *
 * Scope (v1): invoices originated in the app — id not prefixed `INVTLY` (those came *from* Tally),
 * active, numbered, with a named customer, and not already linked to Tally (`ref_id` blank). Each is
 * sent as its own IMPORTDATA request so a single bad voucher (missing ledger/stock item) fails just
 * that invoice; the rest still land. After a push the caller should trigger a normal Tally→local sync
 * so the created voucher is reconciled back (matched by REMOTEID — see [TallyVoucherMapper]).
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
     * Pushes not-yet-pushed local invoices. [log] streams human-readable progress to the shared Tally
     * sync log panel. Returns counts; individual failures are logged, not thrown.
     *
     * @param onlyInvoiceId when non-null, restricts the push to that single invoice (the per-invoice
     *   "Push to Tally" button on the invoice-view screen); when null, pushes every eligible invoice
     *   (the bulk push from the Tally settings screen).
     */
    suspend fun push(workspaceSlug: String, log: (String) -> Unit, onlyInvoiceId: String? = null): TallyPushResult {
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
            (onlyInvoiceId == null || inv.id == onlyInvoiceId) &&   // single-invoice push when set
                inv.soft_deleted == 0L &&
                inv.invoice_number.isNotBlank() &&
                inv.customer_name.isNotBlank() &&
                inv.ref_id.isNullOrBlank() &&        // already linked to a Tally voucher → skip (idempotent)
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
            val result = outcome.getOrNull()   // parsed ImportResult from Tally's <RESPONSE> reply

            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushed += inv.id
                // Capture Tally's reference for this voucher (master id preferred; voucher id as
                // fallback) onto the invoice's ref_id — the durable idempotency marker that also
                // round-trips to the backend. Blank if Tally omitted both (still counts as pushed).
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() }
                    ?: result?.lastVchId?.trim()?.takeIf { it.isNotBlank() }
                if (tallyRef != null) {
                    invoiceDao.setTallyRef(inv.id, tallyRef)
                    log("  ✓ ${inv.invoice_number} → Tally (ref $tallyRef)")
                } else {
                    log("  ✓ ${inv.invoice_number} → Tally")
                }
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: "Tally rejected the voucher (created=${result?.created ?: 0}, errors=${result?.errors ?: 0})"
                log("  ✗ ${inv.invoice_number} — $reason")
                kermitLog.w { "Tally push failed for ${inv.id}: $reason" }
            }
        }

        if (newlyPushed.isNotEmpty()) dataStore.addTallyPushedInvoiceIds(workspaceSlug, newlyPushed)
        log("Tally push complete — pushed=$pushed, failed=$failed")
        return TallyPushResult(pushed = pushed, failed = failed)
    }
}
