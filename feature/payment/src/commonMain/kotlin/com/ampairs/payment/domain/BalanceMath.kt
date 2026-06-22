package com.ampairs.payment.domain

/**
 * The single, platform-independent balance formula (research.md R3/R4, FR-002/022).
 *
 * `closing = openingSigned + Σ(active LedgerEntry.signedAmount)`
 *
 * This is byte-for-byte the same computation the backend `BalanceService.recompute()` runs, so the
 * on-device cached balance and the server's authoritative balance never drift (SC-002/006). It is a
 * pure function over the rows — order-independent — so backdated / offline / out-of-order entries
 * never corrupt it (R3).
 *
 * Opening balance is NOT a ledger entry; it is folded in as [PartyBalance.openingSigned].
 */
object BalanceMath {

    /** Signed closing balance for a party from its opening + active ledger entries. */
    fun recompute(openingSigned: Money, entries: List<LedgerEntry>): Money {
        var sum = openingSigned
        for (e in entries) {
            if (!e.active) continue
            sum += e.signedAmount
        }
        return sum
    }

    /** Convenience overload taking the [PartyBalance] (uses its [PartyBalance.openingSigned]). */
    fun recompute(balance: PartyBalance, entries: List<LedgerEntry>): Money =
        recompute(balance.openingSigned, entries.filter { it.partyUid == balance.partyUid })

    /** Tie-out result mirroring the backend recompute-balance contract (FR-022 / SC-002). */
    data class TieOut(
        val openingSigned: Money,
        val totalDebit: Money,
        val totalCredit: Money,
        val recomputed: Money,
    ) {
        /** `opening_signed + total_debit − total_credit == recomputed`. */
        val tieOutOk: Boolean
            get() = (openingSigned + totalDebit - totalCredit).minor == recomputed.minor
    }

    fun tieOut(openingSigned: Money, entries: List<LedgerEntry>): TieOut {
        var debit = Money.ZERO
        var credit = Money.ZERO
        for (e in entries) {
            if (!e.active) continue
            if (e.direction == Direction.DR) debit += e.amount else credit += e.amount
        }
        return TieOut(
            openingSigned = openingSigned,
            totalDebit = debit,
            totalCredit = credit,
            recomputed = recompute(openingSigned, entries),
        )
    }
}
