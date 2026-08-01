package com.ampairs.invoice.spi

/**
 * Snapshot of a finalized invoice handed to downstream listeners (spec 013, R9). Just the fields a
 * party-ledger receivable needs — no entity/JPA leakage.
 */
data class FinalizedInvoice(
    val uid: String,
    val partyUid: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val totalCost: Double,
)

/**
 * SPI fired by [com.ampairs.invoice.db.InvoiceRepository] on a **local** invoice save when the
 * invoice's finalized state changes — never on sync-pulled writes (those go straight to the DAO via
 * the sync delegate, bypassing the repository). This is what makes the bridge instant *and* free of
 * sync churn.
 *
 * Downstream modules plug in with Metro `@ContributesIntoSet(WorkspaceScope::class)`; the payment
 * module implements it to post / reverse the party receivable. The contract lives in the invoice
 * module so the dependency direction stays correct: invoice publishes it, payment implements it
 * (payment → invoice, never the reverse).
 */
interface InvoiceLifecycleListener {
    /** The invoice newly reached the finalized state — post the receivable. */
    suspend fun onFinalized(invoice: FinalizedInvoice)

    /** The invoice left the finalized state (reverted to draft / cancelled) — reverse the receivable. */
    suspend fun onReverted(invoiceUid: String)
}
