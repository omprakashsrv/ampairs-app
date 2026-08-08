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
 *   LEDGERENTRIES.LIST (one per ledger — repeated sibling elements, NOT a single wrapping list)
 *     party ledger   ISPARTYLEDGER=Yes  ISDEEMEDPOSITIVE=Yes  AMOUNT = -(grand total)   [debit]
 *     GST ledger(s)                     ISDEEMEDPOSITIVE=No   AMOUNT = +(tax value)      [credit]
 * ```
 * `LEDGERENTRIES.LIST`, not `ALLLEDGERENTRIES.LIST`, is what Item Invoice mode (`ISINVOICE=Yes` with
 * inventory) expects for the accounting postings — confirmed against a real, successfully-imported
 * Tally export. The wrong tag is accepted by the XML gateway with HTTP 200 but the voucher is silently
 * rejected (`EXCEPTIONS=1`, no `LINEERROR` text).
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

    const val DEFAULT_SALES_LEDGER = "GST Sales"

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
            // RATE must reconcile with AMOUNT (RATE × ACTUALQTY == AMOUNT) or Tally silently rejects
            // the voucher (EXCEPTIONS>0, no LINEERROR text). item.product_price is the tax-INCLUSIVE
            // selling price (workspaces can enable tax-inclusive pricing — see /cmp-practices §12),
            // but AMOUNT below is item.base_price, the pre-tax taxable value. Derive RATE from
            // base_price/quantity so the two always agree, independent of that pricing mode.
            val unitRate = if (item.quantity != 0.0) item.base_price / item.quantity else item.base_price
            val rateStr = if (unit != null) "${money(unitRate)}/$unit" else money(unitRate)
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
        val stateNameForTally = entity.place_of_supply?.trim()?.takeIf { it.isNotBlank() }?.toTallyStateName()
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
            placeOfSupply = stateNameForTally,
            stateName = stateNameForTally,
            isInvoice = "Yes",
            // Item Invoice mode (ISINVOICE=Yes + inventory) posts party/tax ledgers under repeated
            // LEDGERENTRIES.LIST elements, NOT ALLLEDGERENTRIES.LIST — confirmed against a real,
            // successfully-imported Tally export (Transactions.xml). Using the wrong tag is silently
            // accepted by the XML gateway but the voucher is rejected with EXCEPTIONS=1 and no
            // LINEERROR text, since ALLLEDGERENTRIES.LIST is the plain-voucher (non-invoice) tag.
            ledgerEntrieList = ledgerEntries,
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

    /**
     * `Invoice.place_of_supply` app-wide stores the 2-digit GST state code (see
     * `OrderViewModel.placeOfSupply = ScenarioResolver.stateCodeFromGstin(...)`), not a name — but
     * Tally's `STATENAME`/`PLACEOFSUPPLY` master only resolves actual state names ("Karnataka"), so a
     * bare code ("29") silently fails GST/state resolution (Tally rejects with EXCEPTIONS>0 and no
     * LINEERROR text). Convert a numeric code to its official GST state name; a non-numeric value is
     * assumed to already be a name (some invoices — e.g. ones pulled in from Tally — carry the name
     * directly) and is passed through unchanged.
     */
    private fun String.toTallyStateName(): String =
        toIntOrNull()?.let { GST_STATE_CODE_TO_NAME[it] } ?: this

    /** Official India GST state/UT codes → the state name as registered in Tally's default masters. */
    private val GST_STATE_CODE_TO_NAME = mapOf(
        1 to "Jammu & Kashmir", 2 to "Himachal Pradesh", 3 to "Punjab", 4 to "Chandigarh",
        5 to "Uttarakhand", 6 to "Haryana", 7 to "Delhi", 8 to "Rajasthan", 9 to "Uttar Pradesh",
        10 to "Bihar", 11 to "Sikkim", 12 to "Arunachal Pradesh", 13 to "Nagaland", 14 to "Manipur",
        15 to "Mizoram", 16 to "Tripura", 17 to "Meghalaya", 18 to "Assam", 19 to "West Bengal",
        20 to "Jharkhand", 21 to "Odisha", 22 to "Chhattisgarh", 23 to "Madhya Pradesh",
        24 to "Gujarat", 25 to "Daman & Diu", 26 to "Dadra & Nagar Haveli", 27 to "Maharashtra",
        28 to "Andhra Pradesh (Old)", 29 to "Karnataka", 30 to "Goa", 31 to "Lakshadweep",
        32 to "Kerala", 33 to "Tamil Nadu", 34 to "Puducherry", 35 to "Andaman & Nicobar Islands",
        36 to "Telangana", 37 to "Andhra Pradesh", 38 to "Ladakh",
        97 to "Other Territory", 99 to "Centre Jurisdiction",
    )
}
