package com.ampairs.tax.calculation.document

/**
 * Place-of-supply → [TaxScenario] resolution (spec 010 FR-004).
 *
 * Same state ⇒ INTRA (CGST + SGST). Different state ⇒ INTER (IGST). The Indian state code is the
 * first two digits of the GSTIN. This fixes the legacy inverted `TaxSpec` logic.
 */
object ScenarioResolver {

    /** First two digits of a GSTIN as the state code, or null if not derivable. */
    fun stateCodeFromGstin(gstin: String?): Int? {
        val g = gstin?.trim().orEmpty()
        if (g.length < 2) return null
        val prefix = g.substring(0, 2)
        return if (prefix.all { it.isDigit() }) prefix.toInt() else null
    }

    /**
     * Resolve the scenario from already-derived state codes. A missing/unknown buyer state defaults
     * to INTRA against the seller (per spec edge case: unregistered buyer → intra-state).
     */
    fun resolve(sellerStateCode: Int?, buyerStateCode: Int?): TaxScenario {
        if (sellerStateCode == null || buyerStateCode == null) return TaxScenario.INTRA
        return if (sellerStateCode == buyerStateCode) TaxScenario.INTRA else TaxScenario.INTER
    }

    /**
     * Convenience overload resolving directly from GSTINs with a fallback buyer state (e.g. from the
     * customer's address) when the buyer has no GSTIN. Falls back to the seller state ⇒ INTRA.
     */
    fun resolveFromGstins(
        sellerGstin: String?,
        buyerGstin: String?,
        buyerStateFallback: Int? = null,
    ): TaxScenario {
        val seller = stateCodeFromGstin(sellerGstin)
        val buyer = stateCodeFromGstin(buyerGstin) ?: buyerStateFallback ?: seller
        return resolve(seller, buyer)
    }
}
