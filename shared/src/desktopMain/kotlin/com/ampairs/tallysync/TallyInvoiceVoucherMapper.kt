package com.ampairs.tallysync

import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.invoice.db.model.TaxInfoEntity
import com.ampairs.tally.model.voucher.AccountingAllocation
import com.ampairs.tally.model.voucher.InventoryList
import com.ampairs.tally.model.voucher.LedgerEntrie
import com.ampairs.tally.model.voucher.Voucher
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Builds a Tally **Sales** voucher from a local invoice — the inverse of
 * [TallyVoucherMapper.toMappedInvoice]. This is the push (order → invoice → Tally) direction.
 *
 * The invoice's number and GST are **already computed by the app** (spec 010): the number comes from
 * `sequenceNumberProvider` and rides across as `VOUCHERNUMBER`; the tax split lives in the invoice's
 * `tax_info` and is mapped to **explicit GST ledger lines** (CGST/SGST/IGST) rather than letting Tally
 * recompute it — so the pushed voucher's totals match the app exactly.
 *
 * ### Item-invoice (GST) structure produced
 * ```
 * VOUCHER  VCHTYPE=Sales ACTION=Create REMOTEID=<local invoice id> ISINVOICE=Yes
 *   PARTYLEDGERNAME / PARTYNAME = customer
 *   ALLINVENTORYENTRIES.LIST (one per line)
 *     STOCKITEMNAME, RATE, AMOUNT(+base), ACTUALQTY/BILLEDQTY
 *     ACCOUNTINGALLOCATIONS.LIST → LEDGERNAME=Sales  ISDEEMEDPOSITIVE=No  AMOUNT(+base)
 *   ALLLEDGERENTRIES.LIST
 *     party ledger   ISPARTYLEDGER=Yes  ISDEEMEDPOSITIVE=Yes  AMOUNT = -(grand total)   [debit]
 *     GST ledger(s)                     ISDEEMEDPOSITIVE=No   AMOUNT = +(tax value)      [credit]
 * ```
 * Tally's amount sign convention on import: **debit = negative, credit = positive**; every ledger
 * amount plus the inventory credit must net to zero, which holds because
 * `grandTotal = Σ line base + Σ GST`.
 *
 * ### Preconditions (reported, not auto-fixed — see [TallyInvoicePushService])
 * - The party ledger and every stock item must already exist in Tally by the **exact name** used
 *   here (they were synced *from* Tally, so `customer_name` / item `description` are those names).
 * - The GST output ledgers named in `tax_info` (e.g. "CGST 9%") must exist in Tally.
 * - The Sales voucher type should use manual numbering ("Prevent duplicates") so Tally keeps the
 *   app-assigned [InvoiceEntity.invoice_number] instead of renumbering.
 */
internal object TallyInvoiceVoucherMapper {

    const val DEFAULT_SALES_LEDGER = "Sales"

    /**
     * @param unitNameById resolves an item's `unit_id` to the Tally unit symbol (for RATE/QTY);
     *        a line whose unit is unknown is emitted without a unit token (Tally falls back to the
     *        stock item's base unit).
     */
    fun buildSalesVoucher(
        entity: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        taxComponents: List<TaxInfoEntity>,
        unitNameById: Map<String, String>,
        salesLedgerName: String = DEFAULT_SALES_LEDGER,
    ): Voucher {
        val activeItems = items.filter { it.active == 1L && it.soft_deleted == 0L }

        val inventory = activeItems.map { item ->
            val unit = item.unit_id.takeIf { it.isNotBlank() }?.let { unitNameById[it] }?.trim()?.takeIf { it.isNotBlank() }
            val qtyStr = if (unit != null) "${trimNum(item.quantity)} $unit" else trimNum(item.quantity)
            val rateStr = if (unit != null) "${money(item.product_price)}/$unit" else money(item.product_price)
            InventoryList(
                stockItemName = item.description,
                isDeemedPositive = "No",             // outward (sales) stock movement
                rate = rateStr,
                amount = money(item.base_price),     // credit → positive
                actualQty = qtyStr,
                billedQty = qtyStr,
                accountingAllocationList = listOf(
                    AccountingAllocation(
                        ledgerName = salesLedgerName,
                        isDeemedPositive = "No",     // Sales ledger credited
                        isPartyLedger = "No",
                        amount = money(item.base_price),
                    ),
                ),
            )
        }

        val partyName = entity.customer_name.trim()
        val ledgerEntries = buildList {
            // Party (debtor) is debited with the GST-inclusive grand total → negative amount.
            add(
                LedgerEntrie(
                    ledgerName = partyName,
                    isDeemedPositive = "Yes",
                    isPartyLedger = "Yes",
                    amount = money(-entity.total_cost),
                ),
            )
            // One credit line per GST component the app already computed.
            taxComponents
                .filter { (it.value ?: 0.0) > 0.0 && it.name.isNotBlank() }
                .forEach { comp ->
                    add(
                        LedgerEntrie(
                            ledgerName = comp.name.trim(),
                            isDeemedPositive = "No",
                            isPartyLedger = "No",
                            amount = money(comp.value ?: 0.0),
                        ),
                    )
                }
        }

        val tallyDate = entity.invoice_date.isoToTallyDate()
        return Voucher(
            remoteId = entity.id,
            vchType = "Sales",
            action = "Create",
            date = tallyDate,
            effectiveDate = tallyDate,
            voucherTypeName = "Sales",
            voucherNumber = entity.invoice_number.trim().takeIf { it.isNotBlank() },
            partyLedgerName = partyName,
            partyName = partyName,
            partyGstin = entity.customer_gst.trim().takeIf { it.isNotBlank() },
            placeOfSupply = entity.place_of_supply?.trim()?.takeIf { it.isNotBlank() },
            stateName = entity.place_of_supply?.trim()?.takeIf { it.isNotBlank() },
            isInvoice = "Yes",
            allLedgerEntriesList = ledgerEntries,
            inventoryList = inventory.ifEmpty { null },
            narration = "Ampairs invoice ${entity.invoice_number}".trim(),
        )
    }

    /** "yyyy-MM-dd HH:mm:ss" (or "yyyy-MM-dd") → Tally "yyyyMMdd"; pass through if already 8 digits. */
    private fun String?.isoToTallyDate(): String {
        val s = this?.trim().orEmpty()
        return when {
            s.isEmpty() -> ""
            s.length == 8 && s.all(Char::isDigit) -> s
            s.length >= 10 && s[4] == '-' && s[7] == '-' ->
                s.substring(0, 4) + s.substring(5, 7) + s.substring(8, 10)
            else -> s
        }
    }

    /** Two-decimal money string (debit negative / credit positive); no String.format (KMP-safe). */
    private fun money(v: Double): String {
        val cents = (v * 100).roundToLong()
        val sign = if (cents < 0) "-" else ""
        val a = abs(cents)
        return "$sign${a / 100}.${(a % 100).toString().padStart(2, '0')}"
    }

    /** Quantity with a trailing ".0" trimmed for whole numbers ("3.0" → "3", "1.5" → "1.5"). */
    private fun trimNum(v: Double): String {
        val cents = (v * 100).roundToLong()
        return if (cents % 100 == 0L) "${cents / 100}" else money(v)
    }
}
