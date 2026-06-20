package com.ampairs.payment.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entities for the party ledger (spec 013). Money stored as [Long] minor units (paise);
 * dates as ISO-8601 UTC strings. `synced`/`active` are the standard offline-sync flags
 * (`synced = 0` ⇒ pending push; `active = 0` ⇒ soft-deleted, still pushed in-band).
 */

@Entity(
    tableName = "party_balance",
    indices = [
        Index(value = ["uid"], unique = true, name = "party_balance_uid_idx"),
        Index(value = ["party_uid"], unique = true, name = "party_balance_party_idx"),
    ],
)
data class PartyBalanceEntity(
    @PrimaryKey val uid: String,
    val party_uid: String,
    val opening_minor: Long = 0,
    val opening_direction: String = "DR",
    val opening_as_of: String = "",
    val cached_closing_minor: Long = 0,
    val last_computed_at: String = "",
    val active: Long = 1,
    val synced: Long = 0,
    val updated_at: String? = null,
)

@Entity(
    tableName = "ledger_entry",
    indices = [
        Index(value = ["uid"], unique = true, name = "ledger_entry_uid_idx"),
        Index(value = ["party_uid", "entry_date"], name = "ledger_entry_party_date_idx"),
        Index(value = ["source_type", "source_uid"], name = "ledger_entry_source_idx"),
    ],
)
data class LedgerEntryEntity(
    @PrimaryKey val uid: String,
    val party_uid: String,
    val entry_date: String,
    val entry_type: String,
    val direction: String,
    val amount_minor: Long = 0,
    val source_type: String = "MANUAL",
    val source_uid: String = "",
    val voucher_no: String = "",
    val narration: String? = null,
    val reversal_of: String? = null,
    val reversed: Long = 0,
    val active: Long = 1,
    val synced: Long = 0,
    val updated_at: String? = null,
)

@Entity(
    tableName = "payment_voucher",
    indices = [
        Index(value = ["uid"], unique = true, name = "payment_voucher_uid_idx"),
        Index(value = ["party_uid"], name = "payment_voucher_party_idx"),
    ],
)
data class PaymentVoucherEntity(
    @PrimaryKey val uid: String,
    val party_uid: String,
    val voucher_no: String = "",
    val voucher_date: String,
    val direction: String = "RECEIVED",
    val total_minor: Long = 0,
    val payment_mode: String = "CASH",
    val reference_number: String? = null,
    val instrument_date: String? = null,
    val bank_name: String? = null,
    val clearance_status: String = "CLEARED",
    val unallocated_minor: Long = 0,
    val narration: String? = null,
    val active: Long = 1,
    val synced: Long = 0,
    val updated_at: String? = null,
)

@Entity(
    tableName = "payment_allocation",
    indices = [
        Index(value = ["uid"], unique = true, name = "payment_allocation_uid_idx"),
        Index(value = ["payment_voucher_uid"], name = "payment_allocation_voucher_idx"),
        Index(value = ["target_type", "target_uid"], name = "payment_allocation_target_idx"),
    ],
)
data class PaymentAllocationEntity(
    @PrimaryKey val uid: String,
    val payment_voucher_uid: String,
    val target_type: String = "INVOICE",
    val target_uid: String,
    val amount_minor: Long = 0,
    val active: Long = 1,
    val synced: Long = 0,
    val updated_at: String? = null,
)

@Entity(
    tableName = "adjustment_voucher",
    indices = [
        Index(value = ["uid"], unique = true, name = "adjustment_voucher_uid_idx"),
        Index(value = ["party_uid"], name = "adjustment_voucher_party_idx"),
    ],
)
data class AdjustmentVoucherEntity(
    @PrimaryKey val uid: String,
    val party_uid: String,
    val voucher_no: String = "",
    val voucher_date: String,
    val adjustment_type: String = "ADJUSTMENT",
    val amount_minor: Long = 0,
    val narration: String? = null,
    val source_ref: String? = null,
    val active: Long = 1,
    val synced: Long = 0,
    val updated_at: String? = null,
)
