package com.ampairs.payment.domain

/**
 * Domain models for the party ledger (spec 013, data-model.md). Money carried as [Money] minor
 * units; dates as ISO-8601 UTC strings (storage/sync stay UTC — display converts via AppLocale).
 *
 * These are the in-memory shapes used by ViewModels/UI. Room entities (data/db) store money as
 * `Long` and add `synced`/`active` flags; API models (data/api) carry decimal strings + snake_case.
 */

/** One row per party. A party is an existing customer (partyUid = customer.uid). */
data class PartyBalance(
    val uid: String = "",
    val partyUid: String = "",
    val openingBalance: Money = Money.ZERO,
    val openingDirection: Direction = Direction.DR,
    val openingAsOf: String = "",
    /** Signed; denormalized cache. Server value is authoritative; recompute locally between syncs. */
    val cachedClosingBalance: Money = Money.ZERO,
    val lastComputedAt: String = "",
    val active: Boolean = true,
) {
    /** Opening folded into the balance as a signed contribution (data-model.md). */
    val openingSigned: Money get() = openingBalance.signed(openingDirection)

    /** Closing direction for display (DR = "to receive", CR = "to pay"). */
    val closingDirection: Direction
        get() = if (cachedClosingBalance.minor >= 0L) Direction.DR else Direction.CR
}

/** The posting — sole driver of the party balance. */
data class LedgerEntry(
    val uid: String = "",
    val partyUid: String = "",
    val entryDate: String = "",
    val entryType: EntryType = EntryType.ADJUSTMENT,
    val direction: Direction = Direction.DR,
    /** Always ≥ 0 — never store negatives; the [direction] carries the sign. */
    val amount: Money = Money.ZERO,
    val sourceType: LedgerSourceType = LedgerSourceType.MANUAL,
    val sourceUid: String = "",
    val voucherNo: String = "",
    val narration: String? = null,
    val reversalOf: String? = null,
    val reversed: Boolean = false,
    val active: Boolean = true,
) {
    /** Signed contribution to the party balance. */
    val signedAmount: Money get() = amount.signed(direction)
}

/** Money movement header. Posts exactly one [LedgerEntry] (uid LDG_<voucher.uid>). */
data class PaymentVoucher(
    val uid: String = "",
    val partyUid: String = "",
    val voucherNo: String = "",
    val voucherDate: String = "",
    val direction: PaymentDirection = PaymentDirection.RECEIVED,
    val totalAmount: Money = Money.ZERO,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val referenceNumber: String? = null,
    val instrumentDate: String? = null,
    val bankName: String? = null,
    val clearanceStatus: ClearanceStatus = ClearanceStatus.CLEARED,
    val unallocatedAmount: Money = Money.ZERO,
    val narration: String? = null,
    val active: Boolean = true,
) {
    /** The ledger entry type this voucher posts: RECEIVED ⇒ PAYMENT_IN (CR), PAID ⇒ PAYMENT_OUT (DR). */
    val ledgerEntryType: EntryType
        get() = if (direction == PaymentDirection.RECEIVED) EntryType.PAYMENT_IN else EntryType.PAYMENT_OUT

    val isTerminal: Boolean
        get() = clearanceStatus == ClearanceStatus.BOUNCED || clearanceStatus == ClearanceStatus.CANCELLED
}

/** Matching link — drives open-bills/aging only; never affects the party total. */
data class PaymentAllocation(
    val uid: String = "",
    val paymentVoucherUid: String = "",
    val targetType: AllocationTarget = AllocationTarget.INVOICE,
    val targetUid: String = "",
    val amount: Money = Money.ZERO,
    val active: Boolean = true,
)

/** Non-payment movement. Posts one [LedgerEntry] of the mapped type (uid LDG_<adjustment.uid>). */
data class AdjustmentVoucher(
    val uid: String = "",
    val partyUid: String = "",
    val voucherNo: String = "",
    val voucherDate: String = "",
    val adjustmentType: AdjustmentType = AdjustmentType.ADJUSTMENT,
    val amount: Money = Money.ZERO,
    val narration: String? = null,
    val sourceRef: String? = null,
    val active: Boolean = true,
)

/** A rendered party-statement line (synthetic opening line first, then ledger entries in date order). */
data class StatementLine(
    val entryDate: String,
    val entryType: EntryType?,
    val isOpening: Boolean,
    val voucherNo: String,
    val narration: String?,
    /** Signed running balance after this line. */
    val runningBalance: Money,
    val debit: Money,
    val credit: Money,
)

/** A party's open (unpaid) bill with aging classification. */
data class OpenBill(
    val billUid: String,
    val billNo: String,
    val billDate: String,
    val total: Money,
    val allocated: Money,
    val outstanding: Money,
    val dueDate: String?,
    val daysOverdue: Int,
    val agingBucket: String,
)
