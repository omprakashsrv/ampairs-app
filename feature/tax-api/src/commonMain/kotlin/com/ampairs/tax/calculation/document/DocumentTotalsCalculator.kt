package com.ampairs.tax.calculation.document

import kotlin.math.floor

/**
 * Pure document totals engine for Order/Invoice (spec 010, §4 of plan.md).
 *
 * Ordering is always: **line discount → overall-discount apportionment → tax** (never tax then
 * discount). Supports tax-exclusive/inclusive pricing (C1) and pre-tax-apportioned vs
 * post-tax-reduction overall discounts (C2). All amounts are rounded half-up to 2 decimals; per-line
 * total = taxable + Σ components, so the document totals always reconcile to the line breakdown.
 */
object DocumentTotalsCalculator {

    fun calculate(input: DocumentCalcInput): DocumentCalcResult {
        // --- step 1-2: gross + line discount ---
        val stages = input.lines.map { line ->
            val gross = round2(line.unitPrice * line.quantity)
            val requested = when (line.lineDiscount?.kind) {
                DiscountKind.PERCENT -> round2(gross * (line.lineDiscount.amount / 100.0))
                DiscountKind.FLAT -> line.lineDiscount.amount
                null -> 0.0
            }.coerceAtLeast(0.0)
            val lineDisc = requested.coerceAtMost(gross)
            LineStage(
                line = line,
                gross = gross,
                lineDiscount = lineDisc,
                netAfterLine = round2(gross - lineDisc),
                discountRequestedExceededGross = requested > gross,
            )
        }

        val sumNet = round2(stages.sumOf { it.netAfterLine })

        // --- overall discount value (pre-tax basis; the post-tax basis is the grand, resolved below) ---
        val overall = if (input.overallDiscountMode == OverallDiscountMode.PRE_TAX_APPORTIONED) {
            when (input.overallDiscount?.kind) {
                DiscountKind.PERCENT -> round2(sumNet * (input.overallDiscount.amount / 100.0))
                DiscountKind.FLAT -> input.overallDiscount.amount
                null -> 0.0
            }.coerceIn(0.0, sumNet)
        } else 0.0

        // --- step 3: apportion overall across lines (PRE_TAX only) ---
        val allocations = DoubleArray(stages.size)
        if (input.overallDiscountMode == OverallDiscountMode.PRE_TAX_APPORTIONED && overall > 0.0 && sumNet > 0.0) {
            val largest = stages.indices.maxByOrNull { stages[it].netAfterLine } ?: 0
            var allocated = 0.0
            stages.indices.forEach { i ->
                if (i != largest) {
                    val a = round2(overall * (stages[i].netAfterLine / sumNet))
                    allocations[i] = a
                    allocated = round2(allocated + a)
                }
            }
            // remainder to the largest line so the parts sum exactly to `overall`
            allocations[largest] = round2(overall - allocated)
        }

        // --- step 4-6: per-line taxable, components, totals ---
        val lineResults = stages.mapIndexed { i, s ->
            val netTaxableInput = (s.netAfterLine - allocations[i]).coerceAtLeast(0.0).let(::round2)
            val rate = input.rates[s.line.taxCode] ?: ResolvedRate.EXEMPT

            val taxable: Double
            val totalTax: Double
            if (rate.totalRate <= 0.0) {
                taxable = netTaxableInput
                totalTax = 0.0
            } else if (input.priceMode == PriceMode.TAX_INCLUSIVE) {
                taxable = round2(netTaxableInput / (1.0 + rate.totalRate / 100.0))
                totalTax = round2(netTaxableInput - taxable) // reconciles exactly to the inclusive amount
            } else {
                taxable = netTaxableInput
                totalTax = round2(netTaxableInput * (rate.totalRate / 100.0))
            }

            val components = splitTax(totalTax, rate.components)
            LineCalcResult(
                id = s.line.id,
                gross = s.gross,
                lineDiscountValue = s.lineDiscount,
                overallDiscountAllocated = allocations[i],
                taxable = taxable,
                components = components,
                totalTax = totalTax,
                lineTotal = round2(taxable + totalTax),
                taxableFlooredAtZero = s.discountRequestedExceededGross || (allocations[i] > 0.0 && netTaxableInput == 0.0),
            )
        }

        // --- document aggregation ---
        val taxableSubtotal = round2(lineResults.sumOf { it.taxable })
        val lineDiscountTotal = round2(stages.sumOf { it.lineDiscount })
        val totalTax = round2(lineResults.sumOf { it.totalTax })

        val grouped = LinkedHashMap<String, Double>()
        lineResults.forEach { lr -> lr.components.forEach { c -> grouped[c.name] = round2((grouped[c.name] ?: 0.0) + c.amount) } }
        val taxComponents = grouped.map { (name, amt) -> DocumentTaxComponent(name, amt) }

        val lineTotalSum = round2(lineResults.sumOf { it.lineTotal })
        // POST_TAX_REDUCTION subtracts from the grand total, so a percent discount resolves against
        // the tax-inclusive grand (spec C2), not the pre-tax net.
        val postTaxOverall = if (input.overallDiscountMode == OverallDiscountMode.POST_TAX_REDUCTION) {
            when (input.overallDiscount?.kind) {
                DiscountKind.PERCENT -> round2(lineTotalSum * (input.overallDiscount.amount / 100.0))
                DiscountKind.FLAT -> input.overallDiscount.amount
                null -> 0.0
            }.coerceIn(0.0, lineTotalSum)
        } else 0.0
        val grandTotal = round2(lineTotalSum - postTaxOverall)

        return DocumentCalcResult(
            lines = lineResults,
            taxableSubtotal = taxableSubtotal,
            lineDiscountTotal = lineDiscountTotal,
            overallDiscountValue = if (input.overallDiscountMode == OverallDiscountMode.POST_TAX_REDUCTION) postTaxOverall else overall,
            taxComponents = taxComponents,
            totalTax = totalTax,
            grandTotal = grandTotal,
        )
    }

    /**
     * Split a line's total tax across its components by their share of the total rate, assigning any
     * rounding remainder to the largest-rate component so Σ components == [total] exactly.
     */
    private fun splitTax(total: Double, comps: List<TaxComponentRate>): List<TaxComponentAmount> {
        if (comps.isEmpty()) return emptyList()
        val totalRate = comps.sumOf { it.percentage }
        if (total == 0.0 || totalRate <= 0.0) return comps.map { TaxComponentAmount(it.name, it.percentage, 0.0) }
        val largest = comps.indices.maxByOrNull { comps[it].percentage } ?: 0
        var allocated = 0.0
        val out = comps.mapIndexed { i, c ->
            if (i == largest) {
                TaxComponentAmount(c.name, c.percentage, 0.0) // filled after the loop
            } else {
                val a = round2(total * (c.percentage / totalRate))
                allocated = round2(allocated + a)
                TaxComponentAmount(c.name, c.percentage, a)
            }
        }.toMutableList()
        out[largest] = out[largest].copy(amount = round2(total - allocated))
        return out
    }

    private data class LineStage(
        val line: LineCalcInput,
        val gross: Double,
        val lineDiscount: Double,
        val netAfterLine: Double,
        val discountRequestedExceededGross: Boolean,
    )
}

/** Half-up rounding to 2 decimals. Amounts in this engine are non-negative. */
internal fun round2(value: Double): Double = floor(value * 100.0 + 0.5 + 1e-9) / 100.0
