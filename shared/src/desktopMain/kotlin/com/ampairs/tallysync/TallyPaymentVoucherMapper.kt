package com.ampairs.tallysync

import com.ampairs.payment.data.db.entity.PaymentAllocationEntity
import com.ampairs.payment.data.db.entity.PaymentVoucherEntity
import com.ampairs.payment.domain.Money
import com.ampairs.tally.model.voucher.BankAllocation
import com.ampairs.tally.model.voucher.BillAllocation
import com.ampairs.tally.model.voucher.LedgerEntrie
import com.ampairs.tally.model.voucher.Voucher

/**
 * Builds a Tally **Receipt** (money in) or **Payment** (money out) voucher from a local
 * [PaymentVoucherEntity] + its [PaymentAllocationEntity] rows — the push (collection → Tally)
 * counterpart to [TallyVoucherMapper.toMappedPayment] (the pull direction).
 *
 * ### Structure produced (plain, non-invoice voucher)
 * ```
 * VOUCHER  VCHTYPE=Receipt|Payment ACTION=Create REMOTEID=<local voucher uid> ISINVOICE=No
 *   PARTYLEDGERNAME / PARTYNAME = customer
 *   ALLLEDGERENTRIES.LIST (two sibling elements — the plain-voucher accounting tag; NOT
 *     LEDGERENTRIES.LIST, which Item Invoice mode uses — confirmed against a real, successfully
 *     imported Tally "Payment" voucher export, Transactions.xml)
 *     party ledger    ISPARTYLEDGER=Yes  BILLALLOCATIONS.LIST per settled invoice (BILLTYPE=Agst Ref)
 *     cash/bank ledger BANKALLOCATIONS.LIST when payment_mode != CASH
 * ```
 * Amount sign convention (same as [TallyInvoiceVoucherMapper]): debit = negative, credit = positive;
 * `ISDEEMEDPOSITIVE` = "Yes" ⇒ debit, "No" ⇒ credit. A Receipt credits the party (reduces what they
 * owe) and debits cash/bank; a Payment does the reverse. Each `BILLALLOCATIONS.LIST` amount carries
 * the **same sign** as the party line it belongs to (confirmed against the same real export).
 *
 * ### Preconditions (enforced by [TallyPaymentPushService], not here)
 * - Every allocation's target invoice must already be linked to Tally (`InvoiceEntity.ref_id` set)
 *   so `BILLALLOCATIONS.NAME` (the invoice number) matches a real open bill in Tally.
 * - The voucher must be fully allocated (`unallocated_minor == 0`) — v1 has no "New Ref"/advance
 *   handling for an on-account remainder.
 * - The cash/bank ledger name must exist in Tally by the exact configured name.
 *
 * ### Scope (v1)
 * `payment_mode == CASH` is the most exercised path (no [BankAllocation] needed at all — most
 * small-business collections are cash). Cheque/bank-transfer modes attach a best-effort
 * [BankAllocation]; Tally's bank-reconciliation fields beyond date/instrument/amount are not set.
 */
internal object TallyPaymentVoucherMapper {

    fun buildPaymentVoucher(
        entity: PaymentVoucherEntity,
        allocations: List<PaymentAllocationEntity>,
        partyName: String,
        invoiceNumberByTargetUid: Map<String, String>,
        cashLedgerName: String,
        bankLedgerName: String,
    ): Voucher {
        val isReceived = entity.direction == "RECEIVED"
        val partyName = partyName.trim()
        val counterLedgerName = if (entity.payment_mode == "CASH") {
            cashLedgerName
        } else {
            entity.bank_name?.trim()?.takeIf { it.isNotBlank() } ?: bankLedgerName
        }

        // Credit = positive, debit = negative. Receipt credits the party; Payment debits it.
        val partyAmount = Money(if (isReceived) entity.total_minor else -entity.total_minor)
        val counterAmount = Money(-partyAmount.minor)

        val billAllocations = allocations.mapNotNull { alloc ->
            val billName = invoiceNumberByTargetUid[alloc.target_uid]?.trim()?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val billAmount = Money(if (isReceived) alloc.amount_minor else -alloc.amount_minor)
            BillAllocation(
                name = billName,
                billType = "Agst Ref",
                amount = billAmount.toDecimalString(),
            )
        }

        val tallyDate = entity.voucher_date.isoToTallyDate()

        val partyLine = LedgerEntrie(
            ledgerName = partyName,
            isDeemedPositive = if (isReceived) "No" else "Yes",
            isPartyLedger = "Yes",
            amount = partyAmount.toDecimalString(),
            billAllocationList = billAllocations.ifEmpty { null },
        )
        val counterLine = LedgerEntrie(
            ledgerName = counterLedgerName,
            isDeemedPositive = if (isReceived) "Yes" else "No",
            isPartyLedger = "No",
            amount = counterAmount.toDecimalString(),
            bankAllocationList = if (entity.payment_mode == "CASH") {
                null
            } else {
                listOf(buildBankAllocation(entity, tallyDate, counterAmount))
            },
        )

        val vchType = if (isReceived) "Receipt" else "Payment"
        return Voucher(
            remoteId = entity.uid,
            vchType = vchType,
            action = "Create",
            date = tallyDate,
            effectiveDate = tallyDate,
            voucherTypeName = vchType,
            voucherNumber = entity.voucher_no.trim().takeIf { it.isNotBlank() },
            partyLedgerName = partyName,
            partyName = partyName,
            isInvoice = "No",
            allLedgerEntriesList = listOf(partyLine, counterLine),
            narration = entity.narration?.trim()?.takeIf { it.isNotBlank() }
                ?: "Ampairs ${vchType.lowercase()} ${entity.voucher_no}".trim(),
        )
    }

    private fun buildBankAllocation(entity: PaymentVoucherEntity, tallyDate: String, counterAmount: Money): BankAllocation {
        val transactionType = if (entity.payment_mode == "CHEQUE") "Cheque" else "e-Fund Transfer"
        return BankAllocation(
            date = tallyDate,
            instrumentDate = entity.instrument_date?.isoToTallyDate()?.takeIf { it.isNotBlank() } ?: tallyDate,
            transactionType = transactionType,
            bankName = entity.bank_name?.trim()?.takeIf { it.isNotBlank() },
            instrumentNumber = entity.reference_number?.trim()?.takeIf { it.isNotBlank() },
            paymentMode = "Transacted",
            amount = counterAmount.toDecimalString(),
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
}
