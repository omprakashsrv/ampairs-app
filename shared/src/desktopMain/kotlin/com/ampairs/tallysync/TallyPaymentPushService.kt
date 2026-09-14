package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.payment.data.db.dao.PaymentAllocationDao
import com.ampairs.payment.data.db.dao.PaymentVoucherDao
import com.ampairs.payment.data.db.entity.PaymentAllocationEntity
import com.ampairs.payment.data.db.entity.PaymentVoucherEntity
import com.ampairs.tally.TallyApiImpl
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.buildVoucherImport
import com.ampairs.tally.renderTallyXml
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.first

/** Outcome of a Tally payment-voucher-push cycle. */
data class TallyPaymentPushResult(
    val pushed: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val error: String? = null,
) {
    val success get() = error == null
}

private val kermitLog = Logger.withTag("TallyPaymentPush")

/**
 * Pushes locally-recorded customer collections (party ledger, spec 013) *into* Tally as Receipt
 * vouchers — the payment-side counterpart to [TallyInvoicePushService]. Invoices and their payments
 * are linked (a collection settles one or more invoice bills), so this always runs right after the
 * invoice push in [TallySyncScheduler.pushInvoices] — a payment can only be pushed once every invoice
 * it settles is itself linked to Tally (`InvoiceEntity.ref_id` set).
 *
 * **Idempotency** mirrors [TallyInvoicePushService]: the Tally master id lands on
 * `PaymentVoucherEntity.ref_id` via [PaymentVoucherDao.setTallyRef] (skip any voucher that already
 * carries one), and the pushed uid also rides in the [AppPreferencesDataStore] pushed-payment-ids set
 * so [TallySyncService]'s reconciliation pull doesn't re-import our own voucher as a duplicate
 * `PVCHTLY<guid>` (see `TallySyncService.syncVouchers`).
 *
 * Scope (v1): `direction == RECEIVED` (collections) only — `PAID` and supplier-side payments aren't
 * on the party-ledger model yet (`PartyBalance.partyUid` is always a customer id, spec 013 Phase 1).
 * CLEARED vouchers only (a pending cheque shouldn't hit Tally before it clears), fully allocated
 * (`unallocatedAmount == 0` — v1 has no "New Ref"/advance handling), and every allocation must target
 * an [com.ampairs.invoice.db.entity.InvoiceEntity] that already has a non-blank `ref_id`.
 */
@Inject
class TallyPaymentPushService(
    private val engine: HttpClientEngine,
    private val voucherDao: PaymentVoucherDao,
    private val allocationDao: PaymentAllocationDao,
    private val invoiceDao: InvoiceDao,
    private val customerDao: CustomerDao,
    private val dataStore: AppPreferencesDataStore,
) {
    /** Local id prefix that identifies a Tally-originated payment voucher (must never be pushed back). */
    private val tallyOriginatedPrefix = "PVCHTLY"

    suspend fun push(workspaceSlug: String, log: (String) -> Unit): TallyPaymentPushResult {
        val host = dataStore.getTallyHost(workspaceSlug).first()
        if (host.isBlank()) {
            log("Tally payment push: host not configured")
            return TallyPaymentPushResult(error = "Tally host not configured")
        }
        val port = dataStore.getTallyPort(workspaceSlug).first()
        val baseUrl = "http://$host:$port"
        val repo = TallyRepository(TallyApiImpl(engine, baseUrl))

        val pushedIds = dataStore.getTallyPushedPaymentIds(workspaceSlug).first()
        val cashLedger = dataStore.getTallyCashLedger(workspaceSlug).first()
        val bankLedger = dataStore.getTallyBankLedger(workspaceSlug).first()

        val candidates = voucherDao.selectAll().filter { v ->
            v.direction == "RECEIVED" &&
                v.clearance_status == "CLEARED" &&
                v.total_minor > 0L &&
                v.unallocated_minor == 0L &&
                v.ref_id.isNullOrBlank() &&
                v.uid !in pushedIds &&
                !v.uid.startsWith(tallyOriginatedPrefix)
        }
        if (candidates.isEmpty()) {
            log("Tally payment push: no new payment vouchers to push")
            return TallyPaymentPushResult()
        }
        log("Tally payment push: ${candidates.size} voucher(s) to consider — $baseUrl")

        var pushed = 0
        var failed = 0
        var skipped = 0
        val newlyPushed = mutableSetOf<String>()
        for (voucher in candidates) {
            val partyName = customerDao.getCustomerById(voucher.party_uid)?.name?.trim().orEmpty()
            val allocations = allocationDao.getByVoucher(voucher.uid)
            val invoiceByTargetUid = allocations.associate { it.target_uid to invoiceDao.selectById(it.target_uid) }

            val skipReason = skipReasonFor(voucher, partyName, allocations, invoiceByTargetUid)
            if (skipReason != null) {
                skipped++
                log("  – ${voucher.voucher_no.ifBlank { voucher.uid }} skipped — $skipReason")
                continue
            }

            val invoiceNumberByTargetUid = invoiceByTargetUid.mapValues { it.value!!.invoice_number }

            val built = TallyPaymentVoucherMapper.buildPaymentVoucher(
                entity = voucher,
                allocations = allocations,
                partyName = partyName,
                invoiceNumberByTargetUid = invoiceNumberByTargetUid,
                cashLedgerName = cashLedger,
                bankLedgerName = bankLedger,
            )

            log("  → request ${voucher.voucher_no}: ${renderTallyXml(buildVoucherImport(listOf(built)))}")

            val outcome = runCatching { repo.importVouchers(listOf(built)) }
            val result = outcome.getOrNull()
            val ok = outcome.isSuccess && result?.isSuccess == true
            if (ok) {
                pushed++
                newlyPushed += voucher.uid
                val tallyRef = result?.lastMId?.trim()?.takeIf { it.isNotBlank() }
                    ?: result?.lastVchId?.trim()?.takeIf { it.isNotBlank() }
                if (tallyRef != null) {
                    voucherDao.setTallyRef(voucher.uid, tallyRef)
                    log("  ✓ ${voucher.voucher_no} → Tally (ref $tallyRef)")
                } else {
                    log("  ✓ ${voucher.voucher_no} → Tally")
                }
            } else {
                failed++
                val reason = outcome.exceptionOrNull()?.message
                    ?: result?.lineError?.takeIf { it.isNotBlank() }
                    ?: ("Tally created nothing (created=${result?.created ?: 0}, altered=${result?.altered ?: 0}, " +
                        "ignored=${result?.ignored ?: 0}, errors=${result?.errors ?: 0}, exceptions=${result?.exceptions ?: 0}) " +
                        "— likely a ledger-name mismatch or a bill ref Tally can't match; see the log")
                log("  ✗ ${voucher.voucher_no} — $reason")
                kermitLog.w { "Tally payment push failed for ${voucher.uid}: $reason" }
            }
        }

        if (newlyPushed.isNotEmpty()) dataStore.addTallyPushedPaymentIds(workspaceSlug, newlyPushed)
        log("Tally payment push complete — pushed=$pushed, failed=$failed, skipped=$skipped")
        return TallyPaymentPushResult(pushed = pushed, failed = failed, skipped = skipped)
    }

    /** Null when eligible; otherwise the reason this voucher can't be pushed yet. */
    private fun skipReasonFor(
        voucher: PaymentVoucherEntity,
        partyName: String,
        allocations: List<PaymentAllocationEntity>,
        invoiceByTargetUid: Map<String, InvoiceEntity?>,
    ): String? {
        if (partyName.isBlank()) return "no resolvable customer for party_uid=${voucher.party_uid}"
        if (allocations.isEmpty()) return "no active bill allocations"

        val unsupported = allocations.firstOrNull { it.target_type != "INVOICE" }
        if (unsupported != null) return "allocation targets unsupported type ${unsupported.target_type} (v1: invoices only)"

        val unlinked = allocations.mapNotNull { alloc ->
            val invoice = invoiceByTargetUid[alloc.target_uid]
            when {
                invoice == null -> "target invoice ${alloc.target_uid} not found locally"
                invoice.ref_id.isNullOrBlank() -> "invoice ${invoice.invoice_number} not yet linked to Tally"
                else -> null
            }
        }
        if (unlinked.isNotEmpty()) return unlinked.joinToString(", ")

        return null
    }
}
