package com.ampairs.payment.domain

import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.repository.PaymentLedgerPoster
import dev.zacsweers.metro.Inject

/**
 * Invoice → ledger integration (T020 mobile, R9). When an invoice is finalized (`INVOICEED`), the
 * authoring client posts a deterministic `SALES_INVOICE` (DR) ledger entry `LDG_<invoice.uid>` for
 * `total_cost`. Drafts never post (FR-013). On cancel the entry is reversed (soft-deleted), keeping
 * the audit trail (FR-014).
 *
 * Two entry points:
 *  - [postForFinalizedInvoice] — call right after `invoiceRepository.saveInvoice(...)` when status is
 *    `INVOICEED`, so the on-device party balance reflects the sale immediately and offline.
 *  - [backfillFinalizedInvoices] — run by [com.ampairs.payment.sync.LedgerEntrySyncDelegate] to post
 *    entries for any finalized invoice that doesn't yet have one (e.g. created by a lean client).
 *
 * Kept in the payment module (payment depends on invoice, never the reverse — module boundary).
 */
@Inject
class InvoiceLedgerPoster(
    private val invoiceDao: InvoiceDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val poster: PaymentLedgerPoster,
) {

    private val finalizedStatus = "INVOICEED" // InvoiceStatus.INVOICEED (codebase spelling)

    /** Post / update the sales-invoice ledger entry for a finalized invoice; reverse if not finalized. */
    suspend fun postForFinalizedInvoice(
        invoiceUid: String,
        partyUid: String,
        invoiceNumber: String,
        invoiceDate: String,
        totalCost: Double,
        status: String,
    ) {
        if (!status.equals(finalizedStatus, ignoreCase = true)) {
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

    /** Reverse a finalized invoice's receivable when it's reverted/cancelled locally (FR-014). */
    suspend fun reverseForInvoice(invoiceUid: String) = poster.reverseDocumentEntry(invoiceUid)

    /** Backfill ledger entries for finalized invoices missing one. Returns the count posted. */
    suspend fun backfillFinalizedInvoices(): Int {
        var posted = 0
        val finalized = invoiceDao.selectAll().filter { it.status.equals(finalizedStatus, ignoreCase = true) }
        for (inv in finalized) {
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
}
