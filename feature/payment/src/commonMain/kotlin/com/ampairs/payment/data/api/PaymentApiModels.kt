package com.ampairs.payment.data.api

import com.ampairs.payment.data.db.entity.AdjustmentVoucherEntity
import com.ampairs.payment.data.db.entity.LedgerEntryEntity
import com.ampairs.payment.data.db.entity.PartyBalanceEntity
import com.ampairs.payment.data.db.entity.PaymentAllocationEntity
import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.payment.data.db.entity.PaymentVoucherEntity
import com.ampairs.payment.domain.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Normalize a stored date ("yyyy-MM-dd HH:mm:ss" or ISO) to an ISO-8601 Instant the backend parses.
 *  Invoice-derived ledger entries inherit the invoice's space-formatted date; sync must send ISO. */
@OptIn(ExperimentalTime::class)
private fun String.toIsoInstantOrSelf(): String =
    if (isBlank()) this else DateTimeAdapter.fromDateTimeString(this)?.toString() ?: this

/**
 * Normalize a stored date to an ISO-8601 UTC instant the backend deserializes as [Instant]. Voucher
 * dates are stored in the UI's `dd-MM-yyyy` form (e.g. "12-06-2026"), which the server rejects
 * ("Cannot deserialize java.time.Instant from String '12-06-2026'"). Handles ISO instants, ISO local
 * date/date-time, and `dd-MM-yyyy` / `dd/MM/yyyy`; an unrecognized value is returned unchanged rather
 * than corrupted to the epoch.
 */
@OptIn(ExperimentalTime::class)
private fun String.toIsoDateInstant(): String {
    val s = trim()
    if (s.isEmpty()) return this
    runCatching { return Instant.parse(s).toString() }                                              // ISO instant
    runCatching { return LocalDateTime.parse(s.replace(' ', 'T')).toInstant(TimeZone.UTC).toString() } // yyyy-MM-dd HH:mm:ss
    runCatching { return LocalDate.parse(s).atStartOfDayIn(TimeZone.UTC).toString() }                // yyyy-MM-dd
    val p = s.substringBefore(' ').split('-', '/')                                                   // dd-MM-yyyy / dd/MM/yyyy
    if (p.size == 3 && p[2].length == 4) {
        val day = p[0].toIntOrNull(); val month = p[1].toIntOrNull(); val year = p[2].toIntOrNull()
        if (day != null && month != null && year != null) {
            runCatching { return LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toString() }
        }
    }
    return this
}

/**
 * Wire DTOs for the payment `/sync` contracts (spec 013 contracts/payment-sync.md). Money is sent
 * as a decimal string (`DECIMAL(19,4)` on the backend) and converted to/from [Money] minor units at
 * this boundary. `updated_at` drives the incremental pull checkpoint.
 */

@Serializable
data class PartyBalanceApiModel(
    @SerialName("uid") val uid: String = "",
    @SerialName("party_uid") val partyUid: String = "",
    @SerialName("opening_balance") val openingBalance: String = "0.00",
    @SerialName("opening_direction") val openingDirection: String = "DR",
    @SerialName("opening_as_of") val openingAsOf: String? = null,
    @SerialName("cached_closing_balance") val cachedClosingBalance: String = "0.00",
    @SerialName("last_computed_at") val lastComputedAt: String? = null,
    @SerialName("active") val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class LedgerEntryApiModel(
    @SerialName("uid") val uid: String = "",
    @SerialName("party_uid") val partyUid: String = "",
    @SerialName("entry_date") val entryDate: String = "",
    @SerialName("entry_type") val entryType: String = "ADJUSTMENT",
    @SerialName("direction") val direction: String = "DR",
    @SerialName("amount") val amount: String = "0.00",
    @SerialName("source_type") val sourceType: String = "MANUAL",
    @SerialName("source_uid") val sourceUid: String = "",
    @SerialName("voucher_no") val voucherNo: String = "",
    @SerialName("narration") val narration: String? = null,
    @SerialName("reversal_of") val reversalOf: String? = null,
    @SerialName("reversed") val reversed: Boolean = false,
    @SerialName("active") val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class PaymentVoucherApiModel(
    @SerialName("uid") val uid: String = "",
    @SerialName("party_uid") val partyUid: String = "",
    @SerialName("voucher_no") val voucherNo: String = "",
    @SerialName("voucher_date") val voucherDate: String = "",
    @SerialName("direction") val direction: String = "RECEIVED",
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("payment_mode") val paymentMode: String = "CASH",
    @SerialName("reference_number") val referenceNumber: String? = null,
    @SerialName("instrument_date") val instrumentDate: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("clearance_status") val clearanceStatus: String = "CLEARED",
    @SerialName("unallocated_amount") val unallocatedAmount: String = "0.00",
    @SerialName("narration") val narration: String? = null,
    @SerialName("active") val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class PaymentAllocationApiModel(
    @SerialName("uid") val uid: String = "",
    @SerialName("payment_voucher_uid") val paymentVoucherUid: String = "",
    @SerialName("target_type") val targetType: String = "INVOICE",
    @SerialName("target_uid") val targetUid: String = "",
    @SerialName("amount") val amount: String = "0.00",
    @SerialName("active") val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AdjustmentVoucherApiModel(
    @SerialName("uid") val uid: String = "",
    @SerialName("party_uid") val partyUid: String = "",
    @SerialName("voucher_no") val voucherNo: String = "",
    @SerialName("voucher_date") val voucherDate: String = "",
    @SerialName("adjustment_type") val adjustmentType: String = "ADJUSTMENT",
    @SerialName("amount") val amount: String = "0.00",
    @SerialName("narration") val narration: String? = null,
    @SerialName("source_ref") val sourceRef: String? = null,
    @SerialName("active") val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

// ---- entity ↔ api conversions (sync delegates own these) --------------------------------------

fun PartyBalanceEntity.toApi(): PartyBalanceApiModel = PartyBalanceApiModel(
    uid = uid,
    partyUid = party_uid,
    openingBalance = Money(opening_minor).toDecimalString(),
    openingDirection = opening_direction,
    openingAsOf = opening_as_of,
    cachedClosingBalance = Money(cached_closing_minor).toDecimalString(),
    lastComputedAt = last_computed_at,
    active = active == 1L,
)

fun PartyBalanceApiModel.toEntity(synced: Boolean = true): PartyBalanceEntity = PartyBalanceEntity(
    uid = uid,
    party_uid = partyUid,
    opening_minor = Money.fromDecimalString(openingBalance).minor,
    opening_direction = openingDirection,
    opening_as_of = openingAsOf.orEmpty(),
    cached_closing_minor = Money.fromDecimalString(cachedClosingBalance).minor,
    last_computed_at = lastComputedAt.orEmpty(),
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
    updated_at = updatedAt,
)

fun LedgerEntryEntity.toApi(): LedgerEntryApiModel = LedgerEntryApiModel(
    uid = uid,
    partyUid = party_uid,
    entryDate = entry_date.toIsoInstantOrSelf(),
    entryType = entry_type,
    direction = direction,
    amount = Money(amount_minor).toDecimalString(),
    sourceType = source_type,
    sourceUid = source_uid,
    voucherNo = voucher_no,
    narration = narration,
    reversalOf = reversal_of,
    reversed = reversed == 1L,
    active = active == 1L,
)

fun LedgerEntryApiModel.toEntity(synced: Boolean = true): LedgerEntryEntity = LedgerEntryEntity(
    uid = uid,
    party_uid = partyUid,
    entry_date = entryDate,
    entry_type = entryType,
    direction = direction,
    amount_minor = Money.fromDecimalString(amount).minor,
    source_type = sourceType,
    source_uid = sourceUid,
    voucher_no = voucherNo,
    narration = narration,
    reversal_of = reversalOf,
    reversed = if (reversed) 1 else 0,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
    updated_at = updatedAt,
)

fun PaymentVoucherEntity.toApi(): PaymentVoucherApiModel = PaymentVoucherApiModel(
    uid = uid,
    partyUid = party_uid,
    voucherNo = voucher_no,
    voucherDate = voucher_date.toIsoDateInstant(),
    direction = direction,
    totalAmount = Money(total_minor).toDecimalString(),
    paymentMode = payment_mode,
    referenceNumber = reference_number,
    instrumentDate = instrument_date?.toIsoDateInstant(),
    bankName = bank_name,
    clearanceStatus = clearance_status,
    unallocatedAmount = Money(unallocated_minor).toDecimalString(),
    narration = narration,
    active = active == 1L,
)

fun PaymentVoucherApiModel.toEntity(synced: Boolean = true): PaymentVoucherEntity = PaymentVoucherEntity(
    uid = uid,
    party_uid = partyUid,
    voucher_no = voucherNo,
    voucher_date = voucherDate,
    direction = direction,
    total_minor = Money.fromDecimalString(totalAmount).minor,
    payment_mode = paymentMode,
    reference_number = referenceNumber,
    instrument_date = instrumentDate,
    bank_name = bankName,
    clearance_status = clearanceStatus,
    unallocated_minor = Money.fromDecimalString(unallocatedAmount).minor,
    narration = narration,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
    updated_at = updatedAt,
)

fun PaymentAllocationEntity.toApi(): PaymentAllocationApiModel = PaymentAllocationApiModel(
    uid = uid,
    paymentVoucherUid = payment_voucher_uid,
    targetType = target_type,
    targetUid = target_uid,
    amount = Money(amount_minor).toDecimalString(),
    active = active == 1L,
)

fun PaymentAllocationApiModel.toEntity(synced: Boolean = true): PaymentAllocationEntity = PaymentAllocationEntity(
    uid = uid,
    payment_voucher_uid = paymentVoucherUid,
    target_type = targetType,
    target_uid = targetUid,
    amount_minor = Money.fromDecimalString(amount).minor,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
    updated_at = updatedAt,
)

fun AdjustmentVoucherEntity.toApi(): AdjustmentVoucherApiModel = AdjustmentVoucherApiModel(
    uid = uid,
    partyUid = party_uid,
    voucherNo = voucher_no,
    voucherDate = voucher_date.toIsoDateInstant(),
    adjustmentType = adjustment_type,
    amount = Money(amount_minor).toDecimalString(),
    narration = narration,
    sourceRef = source_ref,
    active = active == 1L,
)

fun AdjustmentVoucherApiModel.toEntity(synced: Boolean = true): AdjustmentVoucherEntity = AdjustmentVoucherEntity(
    uid = uid,
    party_uid = partyUid,
    voucher_no = voucherNo,
    voucher_date = voucherDate,
    adjustment_type = adjustmentType,
    amount_minor = Money.fromDecimalString(amount).minor,
    narration = narration,
    source_ref = sourceRef,
    active = if (active) 1 else 0,
    synced = if (synced) 1 else 0,
    updated_at = updatedAt,
)
