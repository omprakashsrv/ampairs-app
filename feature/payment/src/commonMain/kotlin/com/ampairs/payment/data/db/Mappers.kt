package com.ampairs.payment.data.db

import com.ampairs.payment.data.db.entity.AdjustmentVoucherEntity
import com.ampairs.payment.data.db.entity.LedgerEntryEntity
import com.ampairs.payment.data.db.entity.PartyBalanceEntity
import com.ampairs.payment.data.db.entity.PaymentAllocationEntity
import com.ampairs.payment.data.db.entity.PaymentVoucherEntity
import com.ampairs.payment.domain.AdjustmentType
import com.ampairs.payment.domain.AdjustmentVoucher
import com.ampairs.payment.domain.AllocationTarget
import com.ampairs.payment.domain.ClearanceStatus
import com.ampairs.payment.domain.Direction
import com.ampairs.payment.domain.EntryType
import com.ampairs.payment.domain.LedgerEntry
import com.ampairs.payment.domain.LedgerSourceType
import com.ampairs.payment.domain.Money
import com.ampairs.payment.domain.PartyBalance
import com.ampairs.payment.domain.PaymentAllocation
import com.ampairs.payment.domain.PaymentDirection
import com.ampairs.payment.domain.PaymentMode
import com.ampairs.payment.domain.PaymentVoucher

// ---- enum parse helpers (tolerant of unknown server values) ----------------------------------

private inline fun <reified T : Enum<T>> parseEnum(value: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: default

// ---- PartyBalance ----------------------------------------------------------------------------

fun PartyBalanceEntity.toDomain(): PartyBalance = PartyBalance(
    uid = uid,
    partyUid = party_uid,
    openingBalance = Money(opening_minor),
    openingDirection = parseEnum(opening_direction, Direction.DR),
    openingAsOf = opening_as_of,
    cachedClosingBalance = Money(cached_closing_minor),
    lastComputedAt = last_computed_at,
    active = active == 1L,
    synced = synced == 1L,
)

fun PartyBalance.toEntity(synced: Boolean = false): PartyBalanceEntity = PartyBalanceEntity(
    uid = uid,
    party_uid = partyUid,
    opening_minor = openingBalance.minor,
    opening_direction = openingDirection.name,
    opening_as_of = openingAsOf,
    cached_closing_minor = cachedClosingBalance.minor,
    last_computed_at = lastComputedAt,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
)

// ---- LedgerEntry -----------------------------------------------------------------------------

fun LedgerEntryEntity.toDomain(): LedgerEntry = LedgerEntry(
    uid = uid,
    partyUid = party_uid,
    entryDate = entry_date,
    entryType = parseEnum(entry_type, EntryType.ADJUSTMENT),
    direction = parseEnum(direction, Direction.DR),
    amount = Money(amount_minor),
    sourceType = parseEnum(source_type, LedgerSourceType.MANUAL),
    sourceUid = source_uid,
    voucherNo = voucher_no,
    narration = narration,
    reversalOf = reversal_of,
    reversed = reversed == 1L,
    active = active == 1L,
)

fun LedgerEntry.toEntity(synced: Boolean = false): LedgerEntryEntity = LedgerEntryEntity(
    uid = uid,
    party_uid = partyUid,
    entry_date = entryDate,
    entry_type = entryType.name,
    direction = direction.name,
    amount_minor = amount.minor,
    source_type = sourceType.name,
    source_uid = sourceUid,
    voucher_no = voucherNo,
    narration = narration,
    reversal_of = reversalOf,
    reversed = if (reversed) 1 else 0,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
)

// ---- PaymentVoucher --------------------------------------------------------------------------

fun PaymentVoucherEntity.toDomain(): PaymentVoucher = PaymentVoucher(
    uid = uid,
    partyUid = party_uid,
    voucherNo = voucher_no,
    voucherDate = voucher_date,
    direction = parseEnum(direction, PaymentDirection.RECEIVED),
    totalAmount = Money(total_minor),
    paymentMode = parseEnum(payment_mode, PaymentMode.CASH),
    referenceNumber = reference_number,
    instrumentDate = instrument_date,
    bankName = bank_name,
    clearanceStatus = parseEnum(clearance_status, ClearanceStatus.CLEARED),
    unallocatedAmount = Money(unallocated_minor),
    narration = narration,
    active = active == 1L,
)

fun PaymentVoucher.toEntity(synced: Boolean = false): PaymentVoucherEntity = PaymentVoucherEntity(
    uid = uid,
    party_uid = partyUid,
    voucher_no = voucherNo,
    voucher_date = voucherDate,
    direction = direction.name,
    total_minor = totalAmount.minor,
    payment_mode = paymentMode.name,
    reference_number = referenceNumber,
    instrument_date = instrumentDate,
    bank_name = bankName,
    clearance_status = clearanceStatus.name,
    unallocated_minor = unallocatedAmount.minor,
    narration = narration,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
)

// ---- PaymentAllocation -----------------------------------------------------------------------

fun PaymentAllocationEntity.toDomain(): PaymentAllocation = PaymentAllocation(
    uid = uid,
    paymentVoucherUid = payment_voucher_uid,
    targetType = parseEnum(target_type, AllocationTarget.INVOICE),
    targetUid = target_uid,
    amount = Money(amount_minor),
    active = active == 1L,
)

fun PaymentAllocation.toEntity(synced: Boolean = false): PaymentAllocationEntity = PaymentAllocationEntity(
    uid = uid,
    payment_voucher_uid = paymentVoucherUid,
    target_type = targetType.name,
    target_uid = targetUid,
    amount_minor = amount.minor,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
)

// ---- AdjustmentVoucher -----------------------------------------------------------------------

fun AdjustmentVoucherEntity.toDomain(): AdjustmentVoucher = AdjustmentVoucher(
    uid = uid,
    partyUid = party_uid,
    voucherNo = voucher_no,
    voucherDate = voucher_date,
    adjustmentType = parseEnum(adjustment_type, AdjustmentType.ADJUSTMENT),
    amount = Money(amount_minor),
    narration = narration,
    sourceRef = source_ref,
    active = active == 1L,
)

fun AdjustmentVoucher.toEntity(synced: Boolean = false): AdjustmentVoucherEntity = AdjustmentVoucherEntity(
    uid = uid,
    party_uid = partyUid,
    voucher_no = voucherNo,
    voucher_date = voucherDate,
    adjustment_type = adjustmentType.name,
    amount_minor = amount.minor,
    narration = narration,
    source_ref = sourceRef,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
)
