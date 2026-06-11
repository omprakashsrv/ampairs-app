package com.ampairs.invoice.ui

import com.ampairs.tax.calculation.document.OverallDiscountMode
import com.ampairs.tax.calculation.document.PriceMode
import com.ampairs.tax.calculation.document.TaxScenario

/** GST component grouped by name + rate (e.g. CGST @ 9%). */
data class TaxGroupUi(val name: String, val percentage: Double, val amount: Double)

/**
 * UI model for the totals/GST breakdown panel (spec 010, design screen "totals").
 * Shared by the order and invoice editors; rendered by
 * [com.ampairs.invoice.editor.DocTotalsPanel].
 */
data class TotalsUi(
    val subtotalGross: Double = 0.0,
    val lineDiscountTotal: Double = 0.0,
    val overallDiscountValue: Double = 0.0,
    val taxableSubtotal: Double = 0.0,
    val taxGroups: List<TaxGroupUi> = emptyList(),
    val totalTax: Double = 0.0,
    val grandTotal: Double = 0.0,
    val scenario: TaxScenario = TaxScenario.INTRA,
    val priceMode: PriceMode = PriceMode.TAX_EXCLUSIVE,
    val overallDiscountMode: OverallDiscountMode = OverallDiscountMode.POST_TAX_REDUCTION,
    val itemCount: Int = 0,
)
