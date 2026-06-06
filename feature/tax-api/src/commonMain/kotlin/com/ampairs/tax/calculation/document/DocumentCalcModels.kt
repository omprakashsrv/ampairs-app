package com.ampairs.tax.calculation.document

/**
 * Pure, dependency-free calculation model for store-operations Order/Invoice documents.
 *
 * This is the single source of truth for the document math (feature 010): unit-scaled prices in,
 * GST + discount-correct totals out. It is intentionally free of Room/DI/network so it can be
 * unit-tested in isolation and reused by both the order and invoice ViewModels (and later the
 * storefront). The ViewModel resolves the per-line [ResolvedRate]s (see [TaxRuleRateResolver]) and
 * the [TaxScenario] (see [ScenarioResolver]) before calling [DocumentTotalsCalculator].
 *
 * Money is [Double] for this feature (today's pricing model); the Money(minorUnits) migration rides
 * with the pricing feature. All amounts are rounded half-up to 2 decimals at the component level so
 * displayed lines always reconcile to the document totals.
 */

/** Whether the entered unit price excludes or already includes GST (spec C1 / FR-003). */
enum class PriceMode { TAX_EXCLUSIVE, TAX_INCLUSIVE }

/** How a document-level discount interacts with GST (spec C2 / FR-015). */
enum class OverallDiscountMode { PRE_TAX_APPORTIONED, POST_TAX_REDUCTION }

/** Place-of-supply outcome: same state vs different state. */
enum class TaxScenario { INTRA, INTER }

/** A discount expressed either as a percentage of the base or a flat currency amount. */
enum class DiscountKind { PERCENT, FLAT }

data class DiscountInput(val kind: DiscountKind, val amount: Double)

/** One GST component's rate, e.g. CGST @ 9.0. */
data class TaxComponentRate(val name: String, val percentage: Double)

/**
 * The resolved GST rate for a line in the current scenario — the components that apply
 * (CGST+SGST for intra, IGST for inter) and their summed rate. [EXEMPT] = no tax (blank/0% HSN).
 */
data class ResolvedRate(
    val components: List<TaxComponentRate>,
    val totalRate: Double,
) {
    companion object {
        val EXEMPT = ResolvedRate(emptyList(), 0.0)
    }
}

/**
 * A single line, with the price already unit-scaled (per chosen unit) and any manual override
 * applied by the ViewModel. [taxCode] keys into [DocumentCalcInput.rates].
 */
data class LineCalcInput(
    val id: String,
    val taxCode: String,
    val unitPrice: Double,
    val quantity: Double,
    val lineDiscount: DiscountInput? = null,
)

data class DocumentCalcInput(
    val lines: List<LineCalcInput>,
    val priceMode: PriceMode,
    val overallDiscount: DiscountInput? = null,
    val overallDiscountMode: OverallDiscountMode,
    val scenario: TaxScenario,
    /** taxCode -> rate already resolved for [scenario]; missing entries are treated as [ResolvedRate.EXEMPT]. */
    val rates: Map<String, ResolvedRate>,
)

data class TaxComponentAmount(val name: String, val percentage: Double, val amount: Double)

data class LineCalcResult(
    val id: String,
    val gross: Double,
    val lineDiscountValue: Double,
    val overallDiscountAllocated: Double,
    val taxable: Double,
    val components: List<TaxComponentAmount>,
    val totalTax: Double,
    val lineTotal: Double,
    /** true when the requested discount exceeded the line value and taxable was floored at 0. */
    val taxableFlooredAtZero: Boolean,
)

data class DocumentTaxComponent(val name: String, val amount: Double)

data class DocumentCalcResult(
    val lines: List<LineCalcResult>,
    val taxableSubtotal: Double,
    val lineDiscountTotal: Double,
    val overallDiscountValue: Double,
    /** GST components summed across lines, grouped by name, in first-seen order. */
    val taxComponents: List<DocumentTaxComponent>,
    val totalTax: Double,
    val grandTotal: Double,
)
