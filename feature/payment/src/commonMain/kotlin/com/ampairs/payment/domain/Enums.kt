package com.ampairs.payment.domain

import kotlinx.serialization.Serializable

/**
 * Payment & Collection party-ledger enums (spec 013, data-model.md §Enumerations).
 * Mirrors the backend enums one-for-one (wire values are the enum names, serialized as-is).
 */

/** A single dated movement type against a party. */
@Serializable
enum class EntryType {
    SALES_INVOICE,
    SALES_RETURN,
    CREDIT_NOTE,
    PAYMENT_IN,
    PAYMENT_OUT,
    PURCHASE_BILL,
    PURCHASE_RETURN,
    DEBIT_NOTE,
    DISCOUNT_ALLOWED,
    WRITE_OFF,
    ROUND_OFF,
    ADJUSTMENT;

    /**
     * Each [EntryType] has a fixed natural [Direction] (research.md R2). This is the single source
     * of truth used both to validate stored entries and to derive the signed contribution.
     */
    val naturalDirection: Direction
        get() = when (this) {
            SALES_INVOICE,
            PURCHASE_RETURN,
            DEBIT_NOTE,
            PAYMENT_OUT,
            -> Direction.DR

            SALES_RETURN,
            CREDIT_NOTE,
            PAYMENT_IN,
            PURCHASE_BILL,
            DISCOUNT_ALLOWED,
            WRITE_OFF,
            -> Direction.CR

            // ROUND_OFF / ADJUSTMENT carry whichever direction the caller supplies; default DR.
            ROUND_OFF,
            ADJUSTMENT,
            -> Direction.DR
        }
}

/** DR ⇒ +amount (receivable), CR ⇒ −amount (payable). */
@Serializable
enum class Direction { DR, CR }

/** Money in vs money out. */
@Serializable
enum class PaymentDirection { RECEIVED, PAID }

@Serializable
enum class PaymentMode {
    CASH,
    CHEQUE,
    UPI,
    NEFT,
    RTGS,
    IMPS,
    NET_BANKING,
    CARD,
    BANK_TRANSFER,
    WALLET,
    OTHER;

    /** Cash settles instantly; everything else may carry a reference / clearance step. */
    val isInstant: Boolean get() = this == CASH

    /** Cheques capture instrument number + bank + cheque date. */
    val isCheque: Boolean get() = this == CHEQUE
}

@Serializable
enum class ClearanceStatus { PENDING, CLEARED, BOUNCED, CANCELLED }

@Serializable
enum class AdjustmentType {
    SALES_RETURN,
    CREDIT_NOTE,
    PURCHASE_BILL,
    PURCHASE_RETURN,
    DEBIT_NOTE,
    DISCOUNT_ALLOWED,
    WRITE_OFF,
    ADJUSTMENT;

    /** Maps each adjustment to the [EntryType] (and thus [Direction]) it posts. */
    val entryType: EntryType
        get() = when (this) {
            SALES_RETURN -> EntryType.SALES_RETURN
            CREDIT_NOTE -> EntryType.CREDIT_NOTE
            PURCHASE_BILL -> EntryType.PURCHASE_BILL
            PURCHASE_RETURN -> EntryType.PURCHASE_RETURN
            DEBIT_NOTE -> EntryType.DEBIT_NOTE
            DISCOUNT_ALLOWED -> EntryType.DISCOUNT_ALLOWED
            WRITE_OFF -> EntryType.WRITE_OFF
            ADJUSTMENT -> EntryType.ADJUSTMENT
        }

    val direction: Direction get() = entryType.naturalDirection
}

/** Phase 1 uses INVOICE only; PURCHASE/LEDGER_ENTRY are Phase 2. */
@Serializable
enum class AllocationTarget { INVOICE, PURCHASE, LEDGER_ENTRY }

/** Provenance of a [com.ampairs.payment.domain.LedgerEntry]. */
@Serializable
enum class LedgerSourceType { INVOICE, PAYMENT, ADJUSTMENT, PURCHASE, MANUAL }
