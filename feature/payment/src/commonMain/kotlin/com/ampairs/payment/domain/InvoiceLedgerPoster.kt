package com.ampairs.payment.domain

import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.repository.PaymentLedgerPoster
import dev.zacsweers.metro.Inject

/**
 * Invoice → ledger integration (T020 mobile, R9). When an invoice is **billed** (any saved status
 * other than `DRAFT` — the mobile app saves invoices as `NEW`), the authoring client posts a
 * deterministic `SALES_INVOICE` (DR) ledger entry `LDG_<invoice.uid>` for `total_cost`. Drafts never
 * post (FR-013). On revert-to-draft / cancel the entry is reversed (soft-deleted), audit retained
 * (FR-014).
 *
 * Two entry points:
 *  - [postForFinalizedInvoice] — called from the invoice save SPI the moment an invoice is billed,
 *    so the on-device party balance reflects the sale immediately and offline.
 *  - [backfillFinalizedInvoices] — run by [com.ampairs.payment.sync.LedgerEntrySyncDelegate] / the
 *    dashboard to post entries for any billed invoice that doesn't yet have one (catch-up).
 *
 * Kept in the payment module (payment depends on invoice, never the reverse — module boundary).
 */
@Inject
class InvoiceLedgerPoster(
    private val invoiceDao: InvoiceDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val poster: PaymentLedgerPoster,
) {

    /**
     * A saved invoice is a real sale (post the receivable) unless it's an explicit DRAFT. The mobile
     * app has no "finalize to INVOICEED" step — invoices are saved as NEW — so gating on a single
     * finalized status would never post. Blank status is treated as draft (don't post).
     */
    private fun isBilled(status: String): Boolean =
        status.isNotBlank() && !status.equals(DRAFT_STATUS, ignoreCase = true)

    /** Post the sales-invoice ledger entry for a billed invoice; reverse it when not billed (draft). */
    suspend fun postForFinalizedInvoice(
        invoiceUid: String,
        partyUid: String,
        invoiceNumber: String,
        invoiceDate: String,
        totalCost: Double,
        status: String,
    ) {
        if (!isBilled(status)) {
            // Draft / cancelled → ensure no active ledger entry remains.
            poster.reverseDocumentEntry(invoiceUid)
            return
        }
        if (partyUid.isBlank()) return
        poster.postDocumentEntry(
            partyUid = partyUid,
            sourceType = LedgerSourceType.INVOICE,
            sourceUid = invoiceUid,
            entryType = EntryType.SALES_INVOICE,
            direction = Direction.DR,
            amount = Money.fromDouble(totalCost),
            entryDate = invoiceDate,
            voucherNo = invoiceNumber,
        )
    }

    /** Reverse a billed invoice's receivable when it's reverted to draft / cancelled locally (FR-014). */
    suspend fun reverseForInvoice(invoiceUid: String) = poster.reverseDocumentEntry(invoiceUid)

    /** Backfill ledger entries for billed invoices missing one. Returns the count posted. */
    suspend fun backfillFinalizedInvoices(): Int {
        var posted = 0
        val billed = invoiceDao.selectAll().filter { isBilled(it.status) }
        for (inv in billed) {
            val existing = ledgerEntryDao.getByUid(poster.ledgerUidFor(inv.id))
            if (existing != null && existing.active == 1L) continue
            if (inv.customer_id.isBlank()) continue
            poster.postDocumentEntry(
                partyUid = inv.customer_id,
                sourceType = LedgerSourceType.INVOICE,
                sourceUid = inv.id,
                entryType = EntryType.SALES_INVOICE,
                direction = Direction.DR,
                amount = Money.fromDouble(inv.total_cost),
                entryDate = inv.invoice_date,
                voucherNo = inv.invoice_number,
            )
            posted++
        }
        return posted
    }

    private companion object {
        const val DRAFT_STATUS = "DRAFT" // InvoiceStatus.DRAFT
    }
}
