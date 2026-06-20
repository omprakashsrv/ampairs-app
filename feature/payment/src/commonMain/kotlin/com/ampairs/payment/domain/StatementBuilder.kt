package com.ampairs.payment.domain

/**
 * Builds a party statement (US2 / FR-004): the synthetic opening line first, then active ledger
 * entries in date order, each with the signed running balance after it. The running balance of the
 * last line equals the party's closing balance (the master assertion, SC-002).
 */
object StatementBuilder {

    data class Statement(
        val partyUid: String,
        val opening: PartyBalance,
        val lines: List<StatementLine>,
        val closingBalance: Money,
        val closingDirection: Direction,
    )

    /**
     * @param entries active ledger entries for the party (any order; sorted here by date).
     * @param from / to optional ISO bounds (inclusive). The opening line is always shown.
     */
    fun build(
        balance: PartyBalance,
        entries: List<LedgerEntry>,
        from: String? = null,
        to: String? = null,
    ): Statement {
        val openingSigned = balance.openingSigned
        val sorted = entries
            .filter { it.active }
            .sortedWith(compareBy({ it.entryDate }, { it.uid }))

        val lines = mutableListOf<StatementLine>()
        // Synthetic opening line.
        lines += StatementLine(
            entryDate = balance.openingAsOf,
            entryType = null,
            isOpening = true,
            voucherNo = "",
            narration = null,
            runningBalance = openingSigned,
            debit = if (balance.openingDirection == Direction.DR) balance.openingBalance else Money.ZERO,
            credit = if (balance.openingDirection == Direction.CR) balance.openingBalance else Money.ZERO,
        )

        var running = openingSigned
        for (e in sorted) {
            running += e.signedAmount
            val inRange = (from == null || e.entryDate >= from) && (to == null || e.entryDate <= to)
            if (!inRange) continue
            lines += StatementLine(
                entryDate = e.entryDate,
                entryType = e.entryType,
                isOpening = false,
                voucherNo = e.voucherNo,
                narration = e.narration,
                runningBalance = running,
                debit = if (e.direction == Direction.DR) e.amount else Money.ZERO,
                credit = if (e.direction == Direction.CR) e.amount else Money.ZERO,
            )
        }

        val closing = BalanceMath.recompute(openingSigned, sorted)
        return Statement(
            partyUid = balance.partyUid,
            opening = balance,
            lines = lines,
            closingBalance = closing,
            closingDirection = if (closing.minor >= 0L) Direction.DR else Direction.CR,
        )
    }
}
