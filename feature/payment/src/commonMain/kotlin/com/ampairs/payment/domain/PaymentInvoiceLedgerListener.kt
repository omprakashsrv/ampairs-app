package com.ampairs.payment.domain

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.invoice.domain.InvoiceStatus
import com.ampairs.invoice.spi.FinalizedInvoice
import com.ampairs.invoice.spi.InvoiceLifecycleListener
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Bridges the invoice lifecycle into the party ledger (spec 013, R9 / FR-013/014). Plugged into the
 * invoice module's [InvoiceLifecycleListener] SPI via Metro multibinding, so it runs the instant an
 * invoice is finalized/reverted **locally** — offline, and without the sync churn a table observer
 * would cause (sync-pulled writes bypass the repository, so they never reach this).
 *
 * Posting is idempotent (deterministic `LDG_<invoice.uid>`); the dashboard/sync backfill remains the
 * catch-up safety net for invoices finalized before the module was opened.
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class PaymentInvoiceLedgerListener(
    private val invoiceLedgerPoster: InvoiceLedgerPoster,
) : InvoiceLifecycleListener {

    override suspend fun onFinalized(invoice: FinalizedInvoice) {
        invoiceLedgerPoster.postForFinalizedInvoice(
            invoiceUid = invoice.uid,
            partyUid = invoice.partyUid,
            invoiceNumber = invoice.invoiceNumber,
            invoiceDate = invoice.invoiceDate,
            totalCost = invoice.totalCost,
            status = InvoiceStatus.INVOICEED.name,
        )
    }

    override suspend fun onReverted(invoiceUid: String) {
        invoiceLedgerPoster.reverseForInvoice(invoiceUid)
    }
}
