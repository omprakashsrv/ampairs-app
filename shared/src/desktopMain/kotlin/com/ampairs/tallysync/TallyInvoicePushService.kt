package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.customer.data.db.CustomerDao
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
    /** Set only for a single-invoice push ([TallyInvoicePushService.push]'s `onlyInvoiceId`) that
     * was excluded for a reason OTHER than "already linked to Tally" (e.g. missing customer_name) —
     * lets the caller show the real cause instead of the generic "already in Tally" message. */
    val skippedReason: String? = null,
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
    private val customerDao: CustomerDao,
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
        val salesLedgerName = dataStore.getTallySalesLedger(workspaceSlug).first()
        val unitNameById = unitDao.getAllUnits().first()
            .associate { it.id to it.shortName.ifBlank { it.name } }

        // Repair invoices whose customer_name is blank but customer_id is set — e.g. an order/
        // invoice whose Customer object was only a stub (uid set, name never resolved) when
        // Invoice.asDatabaseModel() ran. Without this a legitimately-billable invoice is silently
        // excluded by the customer_name check below and never reaches Tally.
        val allInvoices = invoiceDao.selectAll().map { inv ->
            if (inv.customer_name.isBlank() && inv.customer_id.isNotBlank()) {
                val resolvedName = customerDao.getCustomerById(inv.customer_id)?.name?.trim().orEmpty()
                if (resolvedName.isNotBlank()) {
                    invoiceDao.setCustomerName(inv.id, resolvedName)
                    log("Tally push: backfilled customer_name (\"$resolvedName\") for ${inv.invoice_number.ifBlank { inv.id }} from the customer table")
                    inv.copy(customer_name = resolvedName)
                } else inv
            } else inv
        }

        val candidates = allInvoices.filter { inv ->
            (onlyInvoiceId == null || inv.id == onlyInvoiceId) &&   // single-invoice push when set
                inv.soft_deleted == 0L &&
                inv.invoice_number.isNotBlank() &&
                inv.customer_name.isNotBlank() &&
                inv.ref_id.isNullOrBlank() &&        // already linked to a Tally voucher → skip (idempotent)
                inv.id !in pushedIds &&
                tallyOriginatedPrefixes.none { inv.id.startsWith(it) }
        }
        if (candidates.isEmpty()) {
            // Diagnostic: onlyInvoiceId set but excluded — say exactly which filter dropped it,
            // instead of a blanket "nothing to push" that looks identical to "already synced".
            // Distinguish real data problems (blocks the push, caller should surface it) from the
            // legitimate idempotent case (ref_id/pushedIds/INVTLY — genuinely already in Tally).
            if (onlyInvoiceId != null) {
                val inv = allInvoices.firstOrNull { it.id == onlyInvoiceId }
                if (inv == null) {
                    val msg = "Invoice $onlyInvoiceId not found locally"
                    log("Tally push: $msg")
                    return TallyPushResult(skipped = 1, skippedReason = msg)
                }
                val problems = buildList {
                    if (inv.soft_deleted != 0L) add("invoice is soft-deleted")
                    if (inv.invoice_number.isBlank()) add("no invoice_number")
                    if (inv.customer_name.isBlank()) {
                        add("no customer_name (customer_id=${inv.customer_id.ifBlank { "<blank>" }})")
                    }
                }
                val alreadyHandled = buildList {
                    if (!inv.ref_id.isNullOrBlank()) add("already linked to Tally (ref_id=${inv.ref_id})")
                    if (inv.id in pushedIds) add("already in pushedIds cache")
                    if (tallyOriginatedPrefixes.any { inv.id.startsWith(it) }) add("Tally-originated id (INVTLY prefix)")
                }
                return if (problems.isNotEmpty()) {
                    val msg = "Cannot push ${inv.invoice_number.ifBlank { inv.id }} — ${problems.joinToString(", ")}"
                    log("Tally push: $msg")
                    TallyPushResult(skipped = 1, skippedReason = msg)
                } else {
                    log(
                        "Tally push: ${inv.invoice_number} skipped — " +
                            alreadyHandled.ifEmpty { listOf("no matching filter reason (unexpected)") }.joinToString(", "),
                    )
                    TallyPushResult()
                }
            }
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
                salesLedgerName = salesLedgerName,
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
                    ?: ("Tally created nothing (created=${result?.created ?: 0}, altered=${result?.altered ?: 0}, " +
                        "ignored=${result?.ignored ?: 0}, combined=${result?.combined ?: 0}, " +
                        "errors=${result?.errors ?: 0}, exceptions=${result?.exceptions ?: 0}) — likely a duplicate " +
                        "(voucher already in Tally) or a masters/voucher-type mismatch; see 'IMPORTDATA reply' in the log")
                log("  ✗ ${inv.invoice_number} — $reason")
                kermitLog.w { "Tally push failed for ${inv.id}: $reason" }
            }
        }

        if (newlyPushed.isNotEmpty()) dataStore.addTallyPushedInvoiceIds(workspaceSlug, newlyPushed)
        log("Tally push complete — pushed=$pushed, failed=$failed")
        return TallyPushResult(pushed = pushed, failed = failed)
    }
}
