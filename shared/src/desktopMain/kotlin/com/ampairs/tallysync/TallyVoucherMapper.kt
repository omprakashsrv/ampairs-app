package com.ampairs.tallysync

import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.payment.domain.AllocationTarget
import com.ampairs.payment.domain.ClearanceStatus
import com.ampairs.payment.domain.EntryType
import com.ampairs.payment.domain.LedgerSourceType
import com.ampairs.payment.domain.Money
import com.ampairs.payment.domain.PaymentAllocation
import com.ampairs.payment.domain.PaymentDirection
import com.ampairs.payment.domain.PaymentMode
import com.ampairs.payment.domain.PaymentVoucher
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.db.entity.PurchaseItemEntity
import com.ampairs.tally.model.voucher.LedgerEntrie
import com.ampairs.tally.model.voucher.Voucher
import kotlin.math.abs

/**
 * Maps Tally vouchers to the app's invoice + payment models.
 *
 * Sales/Purchase/Credit-Note/Debit-Note vouchers → [InvoiceEntity] (+ line items); Receipt/Payment
 * vouchers → [PaymentVoucher] (+ [PaymentAllocation] against invoices, matched by Tally bill ref).
 * The party ledger entry (`ISPARTYLEDGER = Yes`) carries the document grand total and the bill
 * allocations. Local ids are **derived deterministically from the Tally GUID** so re-syncing a
 * changed voucher upserts in place (no duplicates, no ref_id column / schema migration needed); the
 * stable id also keeps the deterministic `LDG_<id>` ledger entry stable across syncs.
 */
internal object TallyVoucherMapper {

    /** DataStore checkpoint key for the voucher alterId high-water mark. */
    const val ENTITY_VOUCHER = "voucher"

    private const val ID_PREFIX_INVOICE = "INVTLY"
    private const val ID_PREFIX_ITEM = "IITTLY"
    private const val ID_PREFIX_PAYMENT = "PVCHTLY"
    private const val ID_PREFIX_ALLOCATION = "PALTLY"
    private const val ID_PREFIX_PURCHASE = "PURTLY"
    private const val ID_PREFIX_PURCHASE_ITEM = "PITTLY"

    enum class Kind { SALES, CREDIT_NOTE, PURCHASE, DEBIT_NOTE, RECEIPT, PAYMENT, OTHER }

    /** Classify by keyword so custom voucher-type names ("GST Sales", "Cash Receipt") still resolve. */
    fun Voucher.classify(): Kind {
        val t = (voucherTypeName ?: vchType ?: "").lowercase()
        return when {
            t.isBlank() -> Kind.OTHER
            "credit note" in t || "creditnote" in t || "credit-note" in t -> Kind.CREDIT_NOTE
            "debit note" in t || "debitnote" in t || "debit-note" in t -> Kind.DEBIT_NOTE
            "receipt" in t -> Kind.RECEIPT
            "payment" in t -> Kind.PAYMENT
            "purchase" in t -> Kind.PURCHASE
            "sales" in t || "invoice" in t -> Kind.SALES
            else -> Kind.OTHER
        }
    }

    val Kind.isInvoiceKind: Boolean
        get() = this == Kind.SALES || this == Kind.CREDIT_NOTE || this == Kind.PURCHASE || this == Kind.DEBIT_NOTE

    val Kind.isPaymentKind: Boolean
        get() = this == Kind.RECEIPT || this == Kind.PAYMENT

    private fun Kind.invoiceStatus(): String = when (this) {
        Kind.SALES -> "NEW"
        Kind.CREDIT_NOTE -> "CREDIT_NOTE"
        Kind.PURCHASE -> "PURCHASE"
        Kind.DEBIT_NOTE -> "DEBIT_NOTE"
        else -> "NEW"
    }

    fun Kind.ledgerEntryType(): EntryType = when (this) {
        Kind.SALES -> EntryType.SALES_INVOICE
        Kind.CREDIT_NOTE -> EntryType.CREDIT_NOTE
        Kind.PURCHASE -> EntryType.PURCHASE_BILL
        Kind.DEBIT_NOTE -> EntryType.DEBIT_NOTE
        else -> EntryType.SALES_INVOICE
    }

    fun Kind.ledgerSourceType(): LedgerSourceType = when (this) {
        Kind.PURCHASE, Kind.DEBIT_NOTE -> LedgerSourceType.PURCHASE
        else -> LedgerSourceType.INVOICE
    }

    private fun sanitize(guid: String): String = guid.filter { it.isLetterOrDigit() }

    /** Tally amount strings: "-15000.00", " 1250.50 " — keep digits, sign and decimal point. */
    private fun String?.parseAmount(): Double {
        val cleaned = this?.trim()?.filter { it.isDigit() || it == '.' || it == '-' } ?: return 0.0
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /** Tally voucher DATE is "YYYYMMDD"; normalize to ISO "yyyy-MM-dd" (pass through if already dashed). */
    private fun String?.tallyDateToIso(): String {
        val s = this?.trim().orEmpty()
        return when {
            s.isEmpty() -> ""
            s.length == 8 && s.all(Char::isDigit) -> "${s.substring(0, 4)}-${s.substring(4, 6)}-${s.substring(6, 8)}"
            else -> s
        }
    }

    /** Tally quantity strings: "90 No", "-6 BOX", "3.000 KGS" — the first token is the magnitude. */
    private fun String?.parseQty(): Double {
        val first = this?.trim()?.split(Regex("\\s+"))?.firstOrNull() ?: return 0.0
        return first.filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull() ?: 0.0
    }

    /** ALLLEDGERENTRIES.LIST is preferred; fall back to LEDGERENTRIES.LIST for older Tally exports. */
    private fun Voucher.ledgerEntries(): List<LedgerEntrie> =
        (allLedgerEntriesList ?: ledgerEntrieList).orEmpty()

    private fun Voucher.partyLedgerEntry(): LedgerEntrie? =
        ledgerEntries().firstOrNull { it.isPartyLedger.equals("Yes", ignoreCase = true) }

    /** The counterparty name — prefer PARTYLEDGERNAME, else the party ledger entry, else PARTYNAME. */
    fun Voucher.resolvePartyName(): String? =
        partyLedgerName?.trim()?.takeIf { it.isNotBlank() }
            ?: partyLedgerEntry()?.ledgerName?.trim()?.takeIf { it.isNotBlank() }
            ?: partyName?.trim()?.takeIf { it.isNotBlank() }

    /** The deterministic local invoice id this voucher maps to (for cross-referencing payments). */
    fun Voucher.invoiceId(): String? = guid?.takeIf { it.isNotBlank() }?.let { ID_PREFIX_INVOICE + sanitize(it) }

    data class MappedInvoice(
        val entity: InvoiceEntity,
        val items: List<InvoiceItemEntity>,
        val entryType: EntryType,
        val sourceType: LedgerSourceType,
    )

    fun Voucher.toMappedInvoice(
        kind: Kind,
        customerId: String,
        customerName: String,
        productIdByName: Map<String, String>,
    ): MappedInvoice? {
        val guid = guid?.takeIf { it.isNotBlank() } ?: return null
        val invoiceId = ID_PREFIX_INVOICE + sanitize(guid)
        val lines = inventoryList.orEmpty()

        val basePrice = lines.sumOf { abs(it.amount.parseAmount()) }
        val partyAmount = abs(partyLedgerEntry()?.amount.parseAmount())
        // Party-ledger amount is the GST-inclusive grand total; fall back to the line sum if absent.
        val grandTotal = if (partyAmount > 0.0) partyAmount else basePrice
        val totalTax = (grandTotal - basePrice).coerceAtLeast(0.0)
        val totalQuantity = lines.sumOf { abs(it.actualQty.parseQty()) }

        val items = lines.mapIndexed { idx, line ->
            val lineAmount = abs(line.amount.parseAmount())
            val qty = abs(line.actualQty.parseQty())
            val rate = line.rate?.substringBefore("/").parseAmount()
            val name = line.stockItemName?.trim().orEmpty()
            InvoiceItemEntity(
                id = ID_PREFIX_ITEM + sanitize(guid) + "_" + idx,
                description = name,
                product_id = name.takeIf { it.isNotBlank() }?.let { productIdByName[it] } ?: "",
                total_cost = lineAmount,
                base_price = lineAmount,
                product_price = rate,
                total_tax = 0.0,
                invoice_id = invoiceId,
                tax_code = "",
                quantity = qty,
                item_no = (idx + 1).toLong(),
                selling_price = rate,
                mrp = rate,
                dp = rate,
                unit_id = "",
                base_quantity = qty,
            )
        }

        val entity = InvoiceEntity(
            id = invoiceId,
            invoice_number = voucherNumber?.trim()?.takeIf { it.isNotBlank() } ?: sanitize(guid).takeLast(10),
            invoice_date = date.tallyDateToIso(),
            status = kind.invoiceStatus(),
            customer_id = customerId,
            customer_name = customerName,
            customer_gst = partyGstin?.trim().orEmpty(),
            place_of_supply = placeOfSupply?.trim()?.takeIf { it.isNotBlank() } ?: stateName?.trim(),
            total_cost = grandTotal,
            total_tax = totalTax,
            total_items = items.size.toLong(),
            total_quantity = totalQuantity,
            base_price = basePrice,
            active = 1,
            soft_deleted = 0,
            synced = 0,
        )
        return MappedInvoice(entity, items, kind.ledgerEntryType(), kind.ledgerSourceType())
    }

    data class MappedPurchase(
        val entity: PurchaseEntity,
        val items: List<PurchaseItemEntity>,
    )

    /**
     * Maps a Tally Purchase voucher to a [PurchaseEntity] (+ items), the buy-side mirror of
     * [toMappedInvoice]. The id is derived deterministically from the Tally GUID so re-syncing a
     * changed voucher upserts in place. Status is RECEIVED so the buy-side ledger poster records the
     * supplier payable ("To Pay"). The party ledger amount is the GST-inclusive grand total.
     */
    fun Voucher.toMappedPurchase(
        supplierId: String,
        supplierName: String,
        productIdByName: Map<String, String>,
    ): MappedPurchase? {
        val guid = guid?.takeIf { it.isNotBlank() } ?: return null
        val purchaseId = ID_PREFIX_PURCHASE + sanitize(guid)
        val lines = inventoryList.orEmpty()

        val basePrice = lines.sumOf { abs(it.amount.parseAmount()) }
        val partyAmount = abs(partyLedgerEntry()?.amount.parseAmount())
        val grandTotal = if (partyAmount > 0.0) partyAmount else basePrice
        val totalTax = (grandTotal - basePrice).coerceAtLeast(0.0)
        val totalQuantity = lines.sumOf { abs(it.actualQty.parseQty()) }

        val items = lines.mapIndexed { idx, line ->
            val lineAmount = abs(line.amount.parseAmount())
            val qty = abs(line.actualQty.parseQty())
            val rate = line.rate?.substringBefore("/").parseAmount()
            val name = line.stockItemName?.trim().orEmpty()
            PurchaseItemEntity(
                id = ID_PREFIX_PURCHASE_ITEM + sanitize(guid) + "_" + idx,
                description = name,
                product_id = name.takeIf { it.isNotBlank() }?.let { productIdByName[it] } ?: "",
                total_cost = lineAmount,
                base_price = lineAmount,
                product_price = rate,
                total_tax = 0.0,
                purchase_id = purchaseId,
                tax_code = "",
                quantity = qty,
                item_no = idx + 1,
                price = rate,
                mrp = rate,
                dp = rate,
                unit_id = "",
                base_quantity = qty,
            )
        }

        val entity = PurchaseEntity(
            id = purchaseId,
            purchase_number = voucherNumber?.trim()?.takeIf { it.isNotBlank() } ?: sanitize(guid).takeLast(10),
            purchase_date = date.tallyDateToIso(),
            status = "RECEIVED",
            supplier_id = supplierId,
            supplier_name = supplierName,
            supplier_gst = partyGstin?.trim().orEmpty(),
            supplier_invoice_number = referenceNumber?.trim()?.takeIf { it.isNotBlank() },
            place_of_supply = placeOfSupply?.trim()?.takeIf { it.isNotBlank() } ?: stateName?.trim(),
            total_cost = grandTotal,
            total_tax = totalTax,
            total_items = items.size.toLong(),
            total_quantity = totalQuantity,
            base_price = basePrice,
            active = 1,
            soft_deleted = 0,
            synced = 0,
        )
        return MappedPurchase(entity, items)
    }

    data class MappedPayment(
        val voucher: PaymentVoucher,
        val allocations: List<PaymentAllocation>,
    )

    fun Voucher.toMappedPayment(
        kind: Kind,
        partyUid: String,
        invoiceIdByNumber: Map<String, String>,
    ): MappedPayment? {
        val guid = guid?.takeIf { it.isNotBlank() } ?: return null
        val voucherUid = ID_PREFIX_PAYMENT + sanitize(guid)
        val partyEntry = partyLedgerEntry()
        val total = abs(partyEntry?.amount.parseAmount())
        if (total <= 0.0) return null

        val bank = ledgerEntries().firstNotNullOfOrNull { it.bankAllocationList?.firstOrNull() }
        val mode = when {
            bank == null -> PaymentMode.CASH
            (bank.transactionType ?: bank.paymentMode).orEmpty().contains("cheque", ignoreCase = true) -> PaymentMode.CHEQUE
            else -> PaymentMode.BANK_TRANSFER
        }

        val voucher = PaymentVoucher(
            uid = voucherUid,
            partyUid = partyUid,
            voucherNo = voucherNumber?.trim().orEmpty(),
            voucherDate = date.tallyDateToIso(),
            direction = if (kind == Kind.RECEIPT) PaymentDirection.RECEIVED else PaymentDirection.PAID,
            totalAmount = Money.fromDouble(total),
            paymentMode = mode,
            referenceNumber = (bank?.instrumentNumber ?: bank?.uniqueReferenceNumber ?: referenceNumber)
                ?.trim()?.takeIf { it.isNotBlank() },
            instrumentDate = bank?.instrumentDate.tallyDateToIso().takeIf { it.isNotBlank() },
            bankName = bank?.bankName?.trim()?.takeIf { it.isNotBlank() },
            clearanceStatus = ClearanceStatus.CLEARED,
            narration = narration?.trim()?.takeIf { it.isNotBlank() },
            active = true,
        )

        val allocations = partyEntry?.billAllocationList.orEmpty().mapIndexedNotNull { idx, bill ->
            val billName = bill.name?.trim()?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            val invoiceId = invoiceIdByNumber[billName] ?: return@mapIndexedNotNull null
            val amount = abs(bill.amount.parseAmount())
            if (amount <= 0.0) return@mapIndexedNotNull null
            PaymentAllocation(
                uid = ID_PREFIX_ALLOCATION + sanitize(guid) + "_" + idx,
                paymentVoucherUid = voucherUid,
                targetType = AllocationTarget.INVOICE,
                targetUid = invoiceId,
                amount = Money.fromDouble(amount),
                active = true,
            )
        }
        return MappedPayment(voucher, allocations)
    }
}
