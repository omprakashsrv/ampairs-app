package com.ampairs.invoice.editor.entry

import com.ampairs.product.data.ProductDataService
import com.ampairs.product.domain.ProductSummary
import com.ampairs.tax.calculation.document.TaxRateProvider
import com.ampairs.tax.calculation.document.TaxScenario
import com.ampairs.unit.data.repository.UnitOptionsLookup

/**
 * Suspend pipeline behind the command bar: query → parsed entry → catalog candidates →
 * ranked matches → resolved previews (with GST rates for the result rows).
 *
 * Owned by the order/invoice ViewModels (one instance per editor); caches per-product unit
 * options and per-(taxCode, scenario) rates for keystroke-speed matching against Room data.
 */
class ComposerEngine(
    private val products: ProductDataService,
    private val unitOptionsLookup: UnitOptionsLookup,
    private val taxRateProvider: TaxRateProvider,
) {

    data class Computation(
        val parsed: ParsedEntry,
        val matches: List<EntryMatch>,
        val previews: List<EntryPreview>,
        /** taxCode → total GST percent (null when the code has no rule → exempt/unknown). */
        val ratePercents: Map<String, Double?>,
        val noMatch: Boolean,
    )

    private val unitCache = mutableMapOf<String, List<EntryUnit>>()
    private val rateCache = mutableMapOf<Pair<String, TaxScenario>, Double?>()

    suspend fun compute(query: String, scenario: TaxScenario, recents: List<String>): Computation {
        val parsed = EntryParser.parse(query)
        val summaries: List<ProductSummary> = if (parsed.words.isEmpty()) {
            val head = products.searchSummaries("", limit = 30)
            val missingRecents = recents.filter { id -> head.none { it.id == id } }
            val recentSummaries = if (missingRecents.isEmpty()) emptyList() else products.getByIds(missingRecents)
            recentSummaries + head
        } else {
            parsed.words
                .flatMap { w -> products.searchSummaries(w, limit = 50) }
                .distinctBy { it.id }
        }
        val candidates = summaries.map { EntryCandidate(it, unitsFor(it)) }
        val matches = EntryMatcher.match(parsed, candidates, recents)
        val previews = matches.map { EntryMatcher.resolve(it, parsed) }
        val rates = ratePercentsFor(previews.map { it.product.taxCode }, scenario)
        return Computation(
            parsed = parsed,
            matches = matches,
            previews = previews,
            ratePercents = rates,
            noMatch = parsed.words.isNotEmpty() && matches.isEmpty(),
        )
    }

    /** Sellable units for a product, base first; falls back to a 1× pseudo-unit when none resolve. */
    suspend fun unitsFor(product: ProductSummary): List<EntryUnit> =
        unitCache.getOrPut(product.id) {
            unitOptionsLookup.unitsForProduct(product.id, product.baseUnitId)
                .map { option ->
                    EntryUnit(
                        unitId = option.unitId,
                        name = option.shortName.ifBlank { option.name },
                        multiplier = option.multiplier,
                        decimalPlaces = option.decimalPlaces,
                        isBase = option.isBase,
                    )
                }
                .ifEmpty {
                    listOf(
                        EntryUnit(
                            unitId = product.baseUnitId.orEmpty(),
                            name = "",
                            multiplier = 1.0,
                            decimalPlaces = 2,
                            isBase = true,
                        )
                    )
                }
        }

    /** Total GST percent per tax code for [scenario] (cached); null = no rule resolved. */
    suspend fun ratePercentsFor(taxCodes: List<String>, scenario: TaxScenario): Map<String, Double?> {
        val distinct = taxCodes.filter { it.isNotBlank() }.distinct()
        val missing = distinct.filter { (it to scenario) !in rateCache }
        if (missing.isNotEmpty()) {
            val resolved = taxRateProvider.resolveAll(missing, scenario)
            missing.forEach { code -> rateCache[code to scenario] = resolved[code]?.totalRate }
        }
        return distinct.associateWith { rateCache[it to scenario] }
    }

    /** Drop cached units/rates (e.g. after an inline product create or settings change). */
    fun invalidate() {
        unitCache.clear()
        rateCache.clear()
    }
}
