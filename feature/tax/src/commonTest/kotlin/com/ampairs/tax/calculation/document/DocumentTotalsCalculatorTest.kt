package com.ampairs.tax.calculation.document

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden tests for the document totals engine (spec 010). Numbers mirror the worked examples in the
 * feature spec / design handoff so this stays the source of truth for the GST + discount math.
 */
class DocumentTotalsCalculatorTest {

    private val tol = 0.001

    private val gst28intra = ResolvedRate(listOf(TaxComponentRate("CGST", 14.0), TaxComponentRate("SGST", 14.0)), 28.0)
    private val gst28inter = ResolvedRate(listOf(TaxComponentRate("IGST", 28.0)), 28.0)
    private val gst18intra = ResolvedRate(listOf(TaxComponentRate("CGST", 9.0), TaxComponentRate("SGST", 9.0)), 18.0)
    private val gst5intra = ResolvedRate(listOf(TaxComponentRate("CGST", 2.5), TaxComponentRate("SGST", 2.5)), 5.0)

    private fun line(id: String, code: String, price: Double, qty: Double, disc: DiscountInput? = null) =
        LineCalcInput(id = id, taxCode = code, unitPrice = price, quantity = qty, lineDiscount = disc)

    private fun input(
        lines: List<LineCalcInput>,
        rates: Map<String, ResolvedRate>,
        priceMode: PriceMode = PriceMode.TAX_EXCLUSIVE,
        overall: DiscountInput? = null,
        overallMode: OverallDiscountMode = OverallDiscountMode.PRE_TAX_APPORTIONED,
        scenario: TaxScenario = TaxScenario.INTRA,
    ) = DocumentCalcInput(lines, priceMode, overall, overallMode, scenario, rates)

    @Test
    fun intraState_splitsCgstSgst() {
        val r = DocumentTotalsCalculator.calculate(
            input(listOf(line("a", "cement", 385.0, 2.0)), mapOf("cement" to gst28intra))
        )
        val l = r.lines.single()
        assertEquals(770.0, l.taxable, tol)
        assertEquals(107.80, l.components.first { it.name == "CGST" }.amount, tol)
        assertEquals(107.80, l.components.first { it.name == "SGST" }.amount, tol)
        assertEquals(215.60, l.totalTax, tol)
        assertEquals(985.60, l.lineTotal, tol)
        assertEquals(985.60, r.grandTotal, tol)
    }

    @Test
    fun interState_singleIgst_sameLineTotal() {
        val r = DocumentTotalsCalculator.calculate(
            input(listOf(line("a", "cement", 385.0, 2.0)), mapOf("cement" to gst28inter), scenario = TaxScenario.INTER)
        )
        val l = r.lines.single()
        assertEquals(1, l.components.size)
        assertEquals(215.60, l.components.single().amount, tol)
        assertEquals(985.60, l.lineTotal, tol)
    }

    @Test
    fun taxInclusive_extractsGst_andReconciles() {
        val r = DocumentTotalsCalculator.calculate(
            input(listOf(line("a", "cement", 385.0, 1.0)), mapOf("cement" to gst28inter), priceMode = PriceMode.TAX_INCLUSIVE, scenario = TaxScenario.INTER)
        )
        val l = r.lines.single()
        assertEquals(300.78, l.taxable, tol)
        assertEquals(84.22, l.totalTax, tol)
        // components + taxable reconcile back to the inclusive amount exactly
        assertEquals(385.00, l.taxable + l.totalTax, tol)
        assertEquals(385.00, l.lineTotal, tol)
    }

    @Test
    fun lineDiscount_isPreTax() {
        val r = DocumentTotalsCalculator.calculate(
            input(listOf(line("a", "x", 100.0, 2.0, DiscountInput(DiscountKind.PERCENT, 10.0))), mapOf("x" to gst18intra))
        )
        val l = r.lines.single()
        assertEquals(180.0, l.taxable, tol)
        assertEquals(32.40, l.totalTax, tol)
        assertEquals(212.40, l.lineTotal, tol)
    }

    @Test
    fun overallDiscount_preTaxApportioned() {
        val r = DocumentTotalsCalculator.calculate(
            input(
                listOf(line("a", "x", 500.0, 1.0), line("b", "x", 500.0, 1.0)),
                mapOf("x" to gst18intra),
                overall = DiscountInput(DiscountKind.FLAT, 200.0),
                overallMode = OverallDiscountMode.PRE_TAX_APPORTIONED,
            )
        )
        r.lines.forEach { l ->
            assertEquals(400.0, l.taxable, tol)
            assertEquals(72.0, l.totalTax, tol)
        }
        assertEquals(200.0, r.overallDiscountValue, tol)
        assertEquals(800.0, r.taxableSubtotal, tol)
        assertEquals(144.0, r.totalTax, tol)
        assertEquals(944.0, r.grandTotal, tol)
    }

    @Test
    fun overallDiscount_postTaxReduction() {
        val r = DocumentTotalsCalculator.calculate(
            input(
                listOf(line("a", "x", 500.0, 1.0), line("b", "x", 500.0, 1.0)),
                mapOf("x" to gst18intra),
                overall = DiscountInput(DiscountKind.FLAT, 200.0),
                overallMode = OverallDiscountMode.POST_TAX_REDUCTION,
            )
        )
        // full GST per line (no apportionment), discount comes off the grand total
        r.lines.forEach { l -> assertEquals(500.0, l.taxable, tol); assertEquals(90.0, l.totalTax, tol) }
        assertEquals(1000.0, r.taxableSubtotal, tol)
        assertEquals(180.0, r.totalTax, tol)
        assertEquals(980.0, r.grandTotal, tol) // 1180 - 200
    }

    @Test
    fun exemptLine_noTax() {
        val r = DocumentTotalsCalculator.calculate(
            input(listOf(line("a", "blank", 100.0, 3.0)), rates = emptyMap())
        )
        val l = r.lines.single()
        assertTrue(l.components.isEmpty())
        assertEquals(0.0, l.totalTax, tol)
        assertEquals(300.0, l.lineTotal, tol)
    }

    @Test
    fun discountExceedingLine_floorsAtZero() {
        val r = DocumentTotalsCalculator.calculate(
            input(listOf(line("a", "x", 200.0, 1.0, DiscountInput(DiscountKind.FLAT, 250.0))), mapOf("x" to gst18intra))
        )
        val l = r.lines.single()
        assertEquals(0.0, l.taxable, tol)
        assertEquals(0.0, l.totalTax, tol)
        assertTrue(l.taxableFlooredAtZero)
    }

    @Test
    fun multiLine_groupsComponents_andReconciles() {
        // cement 2 BAG @385 (28%) + oil net 7830 (5% via 1740 x5 less 10%) — mirrors the design invoice
        val r = DocumentTotalsCalculator.calculate(
            input(
                listOf(
                    line("a", "cement", 385.0, 2.0),
                    line("b", "oil", 1740.0, 5.0, DiscountInput(DiscountKind.PERCENT, 10.0)),
                ),
                mapOf("cement" to gst28intra, "oil" to gst5intra),
            )
        )
        assertEquals(8600.0, r.taxableSubtotal, tol)
        assertEquals(607.10, r.totalTax, tol)
        assertEquals(303.55, r.taxComponents.first { it.name == "CGST" }.amount, tol)
        assertEquals(303.55, r.taxComponents.first { it.name == "SGST" }.amount, tol)
        assertEquals(9207.10, r.grandTotal, tol)
    }

    @Test
    fun componentsAlwaysSumToLineTax() {
        // odd-ish amount to exercise the remainder-to-largest rule
        val r = DocumentTotalsCalculator.calculate(
            input(listOf(line("a", "x", 333.33, 1.0)), mapOf("x" to gst18intra))
        )
        val l = r.lines.single()
        assertEquals(l.totalTax, l.components.sumOf { it.amount }, tol)
    }
}
