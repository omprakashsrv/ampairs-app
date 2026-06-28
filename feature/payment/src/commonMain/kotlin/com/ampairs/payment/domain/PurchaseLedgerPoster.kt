package com.ampairs.payment.domain

import com.ampairs.purchase.db.dao.PurchaseDao
import com.ampairs.payment.data.db.dao.LedgerEntryDao
import com.ampairs.payment.data.repository.PaymentLedgerPoster
import dev.zacsweers.metro.Inject

/**
 * Purchase → ledger integration (the buy-side mirror of [InvoiceLedgerPoster]). When a purchase is
 * **received** (status RECEIVED), the authoring client posts a deterministic `PURCHASE_BILL` (CR)
 * ledger entry `LDG_<purchase.uid>` for `total_cost`, keyed by the supplier as the party. A CR entry
 * drives the supplier's signed balance negative → the payment dashboard shows it under **"To Pay"**.
 * Non-received purchases (DRAFT/ORDERED/CANCELLED) reverse the entry (soft-delete, audit retained).
 *
 * Kept in the payment module (payment depends on purchase, never the reverse — module boundary),
 * exactly like the invoice integration.
 */
@Inject
class PurchaseLedgerPoster(
    private val purchaseDao: PurchaseDao,
    private val ledgerEntryDao: LedgerEntryDao,
    private val poster: PaymentLedgerPoster,
) {

    /** A purchase is a real payable once received; any other status reverses the entry. */
    private fun isReceived(status: String): Boolean =
        status.equals(RECEIVED_STATUS, ignoreCase = true)

    /** Post the purchase-bill payable for a received purchase; reverse it when not received. */
    suspend fun postForReceivedPurchase(
        purchaseUid: String,
        partyUid: String,
        purchaseNumber: String,
        purchaseDate: String,
        totalCost: Double,
        status: String,
    ) {
        if (!isReceived(status)) {
            poster.reverseDocumentEntry(purchaseUid)
            return
        }
        if (partyUid.isBlank()) return
        poster.postDocumentEntry(
            partyUid = partyUid,
            sourceType = LedgerSourceType.PURCHASE,
            sourceUid = purchaseUid,
            entryType = EntryType.PURCHASE_BILL,
            direction = Direction.CR,
            amount = Money.fromDouble(totalCost),
            entryDate = purchaseDate,
            voucherNo = purchaseNumber,
        )
    }

    /** Reverse a received purchase's payable when it's reverted / cancelled locally. */
    suspend fun reverseForPurchase(purchaseUid: String) = poster.reverseDocumentEntry(purchaseUid)

    /** Backfill ledger entries for received purchases missing one. Returns the count posted. */
    suspend fun backfillReceivedPurchases(): Int {
        var posted = 0
        val received = purchaseDao.selectAll().filter { isReceived(it.status) }
        for (p in received) {
            if (p.supplier_id.isBlank()) continue
            val existing = ledgerEntryDao.getByUid(poster.ledgerUidFor(p.id))
            // Skip only if an active entry already exists with the SAME amount; re-post on amount edits.
            val expectedMinor = Money.fromDouble(p.total_cost).minor
            if (existing != null && existing.active == 1L && existing.amount_minor == expectedMinor) continue
            poster.postDocumentEntry(
                partyUid = p.supplier_id,
                sourceType = LedgerSourceType.PURCHASE,
                sourceUid = p.id,
                entryType = EntryType.PURCHASE_BILL,
                direction = Direction.CR,
                amount = Money.fromDouble(p.total_cost),
                entryDate = p.purchase_date,
                voucherNo = p.purchase_number,
            )
            posted++
        }
        return posted
    }

    private companion object {
        const val RECEIVED_STATUS = "RECEIVED" // PurchaseStatus.RECEIVED
    }
}
