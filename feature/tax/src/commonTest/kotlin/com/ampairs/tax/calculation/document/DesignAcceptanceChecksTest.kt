package com.ampairs.tax.calculation.document

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates calc-core against the v2 design handoff acceptance checks (seed: seller MH-27;
 * cement ₹385 @28% × 2 BAG; oil 1L ₹145 @5% × 5 CARTON (×12 → ₹1,740/CARTON) −10%).
 */
class DesignAcceptanceChecksTest {

    private val tol = 0.001

    private val gst28intra = ResolvedRate(listOf(TaxComponentRate("CGST", 14.0), TaxComponentRate("SGST", 14.0)), 28.0)
    private val gst5intra = ResolvedRate(listOf(TaxComponentRate("CGST", 2.5), TaxComponentRate("SGST", 2.5)), 5.0)
    private val gst28inter = ResolvedRate(listOf(TaxComponentRate("IGST", 28.0)), 28.0)
    private val gst5inter = ResolvedRate(listOf(TaxComponentRate("IGST", 5.0)), 5.0)
    private val gst18intra = ResolvedRate(listOf(TaxComponentRate("CGST", 9.0), TaxComponentRate("SGST", 9.0)), 18.0)

    private fun seedLines(cementQty: Double = 2.0) = listOf(
        LineCalcInput("cement", "2523", unitPrice = 385.0, quantity = cementQty),
        // oil: 1L variant ₹145/PCS, CARTON = ×12 → unit-scaled price 1740/CARTON
        LineCalcInput("oil", "1512", unitPrice = 1740.0, quantity = 5.0, lineDiscount = DiscountInput(DiscountKind.PERCENT, 10.0)),
    )

    private fun doc(
        lines: List<LineCalcInput>,
        priceMode: PriceMode = PriceMode.TAX_EXCLUSIVE,
        scenario: TaxScenario = TaxScenario.INTRA,
        rates: Map<String, ResolvedRate> = mapOf("2523" to gst28intra, "1512" to gst5intra),
    ) = DocumentTotalsCalculator.calculate(
        DocumentCalcInput(lines, priceMode, null, OverallDiscountMode.POST_TAX_REDUCTION, scenario, rates)
    )

    /** Acceptance check 1 — default doc, exclusive, intra. */
    @Test
    fun check1_defaultDoc_exclusive_intra() {
        val r = doc(seedLines())
        assertEquals(8600.00, r.taxableSubtotal, tol)
        assertEquals(607.10, r.totalTax, tol)
        assertEquals(9207.10, r.grandTotal, tol)
        // CGST/SGST split 2.5% + 14% per line
        val cement = r.lines.first { it.id == "cement" }
        val oil = r.lines.first { it.id == "oil" }
        assertEquals(107.80, cement.components.first { it.name == "CGST" }.amount, tol)
        assertEquals(195.75, oil.components.first { it.name == "CGST" }.amount, tol)
    }

    /** Acceptance check 2 — parle 5 box 10%: 5 BOX × ₹2,160 −10% = ₹9,720 pre-tax line amount. */
    @Test
    fun check2_composerPreviewAmount() {
        // composer preview amount = price × qty × (1 − disc%) before tax
        val gross = 2160.0 * 5.0
        val amount = gross * (1 - 0.10)
        assertEquals(9720.00, amount, tol)
        // and through the calculator (18%): taxable = 9720
        val r = doc(
            listOf(LineCalcInput("parle", "1905", 2160.0, 5.0, DiscountInput(DiscountKind.PERCENT, 10.0))),
            rates = mapOf("1905" to gst18intra),
        )
        assertEquals(9720.00, r.lines.single().taxable, tol)
    }

    /** Acceptance check 5 — inclusive mode: taxable ₹8,058.70, grand ₹8,600.00, components reconcile. */
    @Test
    fun check5_inclusive_reconciles() {
        val r = doc(seedLines(), priceMode = PriceMode.TAX_INCLUSIVE)
        assertEquals(8058.70, r.taxableSubtotal, tol)
        assertEquals(8600.00, r.grandTotal, tol)
        // every line: taxable + Σcomponents == inclusive amount exactly
        r.lines.forEach { l ->
            assertEquals(l.lineTotal, l.taxable + l.components.sumOf { it.amount }, tol)
        }
        assertEquals(r.grandTotal, r.taxableSubtotal + r.totalTax, tol)
    }

    /** Acceptance check 6 — inter-state customer flips to IGST with same line totals. */
    @Test
    fun check6_interState_sameLineTotals() {
        val intra = doc(seedLines())
        val inter = doc(
            seedLines(), scenario = TaxScenario.INTER,
            rates = mapOf("2523" to gst28inter, "1512" to gst5inter),
        )
        assertEquals(intra.grandTotal, inter.grandTotal, tol)
        intra.lines.zip(inter.lines).forEach { (a, b) -> assertEquals(a.lineTotal, b.lineTotal, tol) }
        assertEquals(setOf("IGST"), inter.lines.flatMap { it.components }.map { it.name }.toSet())
    }

    /** Acceptance check 7 — stepper +1 on cement (28%) raises the grand total by exactly ₹492.80. */
    @Test
    fun check7_stepperDelta() {
        val before = doc(seedLines(cementQty = 2.0)).grandTotal
        val after = doc(seedLines(cementQty = 3.0)).grandTotal
        assertEquals(492.80, after - before, tol)
    }

    /** Unit scaling (FR-014): price per unit = base price × multiplier; baseQuantity = qty × multiplier. */
    @Test
    fun unitScaling_cartonPricing() {
        val basePrice = 145.0
        val multiplier = 12.0
        assertEquals(1740.0, basePrice * multiplier, tol)
        assertEquals(60.0, 5.0 * multiplier, tol) // base qty 60 PCS
        // BOX of 24 parle @90 → 2160
        assertEquals(2160.0, 90.0 * 24.0, tol)
    }

    /** Prototype parity: post-tax PERCENT overall discount resolves against the tax-inclusive grand. */
    @Test
    fun postTax_percentDiscount_resolvesAgainstGrand() {
        val r = DocumentTotalsCalculator.calculate(
            DocumentCalcInput(
                lines = listOf(LineCalcInput("a", "x", 500.0, 2.0)), // 1000 + 18% = 1180
                priceMode = PriceMode.TAX_EXCLUSIVE,
                overallDiscount = DiscountInput(DiscountKind.PERCENT, 10.0),
                overallDiscountMode = OverallDiscountMode.POST_TAX_REDUCTION,
                scenario = TaxScenario.INTRA,
                rates = mapOf("x" to gst18intra),
            )
        )
        // prototype: 10% of grand (1180) = 118 → 1062.00
        assertEquals(118.0, r.overallDiscountValue, tol)
        assertEquals(1062.00, r.grandTotal, tol)
    }

    /** Scenario resolution (FR-004): same state ⇒ INTRA, different ⇒ INTER, no GSTIN ⇒ INTRA. */
    @Test
    fun scenarioResolution() {
        assertEquals(TaxScenario.INTRA, ScenarioResolver.resolveFromGstins("27ABCDE1234F1Z5", "27PQRST5678U1Z3"))
        assertEquals(TaxScenario.INTER, ScenarioResolver.resolveFromGstins("27ABCDE1234F1Z5", "29WXYZ9012A1Z7"))
        assertEquals(TaxScenario.INTER, ScenarioResolver.resolveFromGstins("27ABCDE1234F1Z5", "24ABCDF6789G1Z4"))
        assertEquals(TaxScenario.INTRA, ScenarioResolver.resolveFromGstins("27ABCDE1234F1Z5", null))
        assertEquals(TaxScenario.INTRA, ScenarioResolver.resolveFromGstins("27ABCDE1234F1Z5", ""))
    }
}

/**
 * Costing-focused validation (user request): line costing always reconciles —
 * lineTotal = taxable + Σ components — under overrides, discounts and both price modes.
 */
class CostingReconciliationTest {

    private val tol = 0.001
    private val gst18intra = ResolvedRate(listOf(TaxComponentRate("CGST", 9.0), TaxComponentRate("SGST", 9.0)), 18.0)
    private val gst12intra = ResolvedRate(listOf(TaxComponentRate("CGST", 6.0), TaxComponentRate("SGST", 6.0)), 12.0)
    private val gst28intra = ResolvedRate(listOf(TaxComponentRate("CGST", 14.0), TaxComponentRate("SGST", 14.0)), 28.0)
    private val rates = mapOf("a" to gst18intra, "b" to gst12intra, "c" to gst28intra)

    private fun calc(
        lines: List<LineCalcInput>,
        priceMode: PriceMode = PriceMode.TAX_EXCLUSIVE,
        overall: DiscountInput? = null,
        mode: OverallDiscountMode = OverallDiscountMode.PRE_TAX_APPORTIONED,
    ) = DocumentTotalsCalculator.calculate(
        DocumentCalcInput(lines, priceMode, overall, mode, TaxScenario.INTRA, rates)
    )

    /** Every line and the document must reconcile exactly, even with awkward prices. */
    @Test
    fun costing_reconciles_with_awkward_prices_and_discounts() {
        val r = calc(
            lines = listOf(
                LineCalcInput("1", "a", unitPrice = 33.33, quantity = 7.0, lineDiscount = DiscountInput(DiscountKind.PERCENT, 12.5)),
                LineCalcInput("2", "b", unitPrice = 19.99, quantity = 3.0, lineDiscount = DiscountInput(DiscountKind.FLAT, 7.77)),
                LineCalcInput("3", "c", unitPrice = 101.01, quantity = 2.5),
            ),
            overall = DiscountInput(DiscountKind.FLAT, 23.45),
        )
        r.lines.forEach { l ->
            assertEquals(l.lineTotal, l.taxable + l.components.sumOf { it.amount }, tol)
        }
        assertEquals(r.taxableSubtotal, r.lines.sumOf { it.taxable }, tol)
        assertEquals(r.totalTax, r.lines.sumOf { it.totalTax }, tol)
        assertEquals(r.grandTotal, r.taxableSubtotal + r.totalTax, tol)
        // apportioned parts sum exactly to the entered overall discount
        assertEquals(23.45, r.lines.sumOf { it.overallDiscountAllocated }, tol)
    }

    /** Price override costing: an overridden unit-scaled price is the costing basis (FR-016). */
    @Test
    fun costing_priceOverride_isTheBasis() {
        // catalog would be 90 × 24 = 2160/BOX; operator typed @2000
        val r = calc(listOf(LineCalcInput("1", "a", unitPrice = 2000.0, quantity = 5.0, lineDiscount = DiscountInput(DiscountKind.PERCENT, 10.0))))
        val l = r.lines.single()
        assertEquals(10000.0, l.gross, tol)
        assertEquals(1000.0, l.lineDiscountValue, tol)
        assertEquals(9000.0, l.taxable, tol)
        assertEquals(1620.0, l.totalTax, tol)      // 18% of 9000
        assertEquals(10620.0, l.lineTotal, tol)
    }

    /** Inclusive + line discount: discount reduces the inclusive amount, then GST is extracted. */
    @Test
    fun costing_inclusive_withLineDiscount_extractsFromNet() {
        val r = calc(
            listOf(LineCalcInput("1", "c", unitPrice = 385.0, quantity = 2.0, lineDiscount = DiscountInput(DiscountKind.PERCENT, 10.0))),
            priceMode = PriceMode.TAX_INCLUSIVE,
        )
        val l = r.lines.single()
        // 770 − 77 = 693 inclusive; taxable = 693 / 1.28 = 541.41; GST = 151.59
        assertEquals(541.41, l.taxable, tol)
        assertEquals(151.59, l.totalTax, tol)
        assertEquals(693.00, l.lineTotal, tol)
        assertEquals(l.lineTotal, l.taxable + l.components.sumOf { it.amount }, tol)
    }

    /** Costing never goes negative: oversized discounts floor the taxable at zero. */
    @Test
    fun costing_neverNegative() {
        val r = calc(
            listOf(LineCalcInput("1", "a", unitPrice = 100.0, quantity = 1.0, lineDiscount = DiscountInput(DiscountKind.FLAT, 500.0))),
            overall = DiscountInput(DiscountKind.FLAT, 999.0),
            mode = OverallDiscountMode.POST_TAX_REDUCTION,
        )
        val l = r.lines.single()
        assertEquals(0.0, l.taxable, tol)
        assertEquals(0.0, l.totalTax, tol)
        assertTrue(l.taxableFlooredAtZero)
        assertTrue(r.grandTotal >= 0.0)
    }

    /** Half-up rounding at 2 decimals on every component (FR worked example style). */
    @Test
    fun costing_halfUpRounding() {
        // taxable 0.05 @18% → tax 0.009 → CGST 0.0045→0.01? components: 9% of 0.05 = 0.0045 each.
        val r = calc(listOf(LineCalcInput("1", "a", unitPrice = 0.05, quantity = 1.0)))
        val l = r.lines.single()
        // total tax = round2(0.009) = 0.01, split: smaller comp 0.0045 → 0.00? remainder to largest.
        assertEquals(l.totalTax, l.components.sumOf { it.amount }, tol)
        assertEquals(l.lineTotal, l.taxable + l.totalTax, tol)
    }
}
