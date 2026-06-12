package com.ampairs.invoice.editor.entry

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

/**
 * Forgiving candidate matching for the fast-entry composer (spec 010 v2 design).
 *
 * Every non-unit word must match the product name (substring, case-insensitive), the HSN/tax code
 * (prefix) or the barcode (substring). A word equal to one of the candidate's unit names selects
 * that unit instead of filtering the name. An empty query shows recents first, then the rest.
 */
object EntryMatcher {

    const val MAX_RESULTS = 5

    fun match(
        parsed: ParsedEntry,
        candidates: List<EntryCandidate>,
        recentProductIds: List<String>,
        limit: Int = MAX_RESULTS,
    ): List<EntryMatch> {
        if (parsed.words.isEmpty()) {
            val byId = candidates.associateBy { it.product.id }
            val recent = recentProductIds.mapNotNull { byId[it] }
            val rest = candidates.filter { it.product.id !in recentProductIds }
            return (recent + rest).take(limit).map {
                EntryMatch(it, unit = null, nameHits = 0, exactBarcode = false, isRecent = it.product.id in recentProductIds)
            }
        }
        val out = mutableListOf<EntryMatch>()
        candidates.forEach { candidate ->
            val name = candidate.product.name.lowercase()
            val hsn = candidate.product.taxCode.lowercase()
            val barcode = candidate.barcode
            var unit: EntryUnit? = null
            var nameHits = 0
            var ok = true
            for (w in parsed.words) {
                val unitMatch = candidate.units.firstOrNull { it.name.equals(w, ignoreCase = true) }
                when {
                    unitMatch != null -> unit = unitMatch
                    name.contains(w) -> nameHits++
                    hsn.isNotEmpty() && hsn.startsWith(w) -> Unit
                    barcode != null && barcode.contains(w) -> Unit
                    else -> { ok = false; break }
                }
            }
            if (ok) {
                val exact = barcode != null && parsed.words.size == 1 && barcode == parsed.words.single()
                out.add(
                    EntryMatch(
                        candidate = candidate,
                        unit = unit,
                        nameHits = nameHits,
                        exactBarcode = exact,
                        isRecent = candidate.product.id in recentProductIds,
                    )
                )
            }
        }
        return out
            .sortedWith(compareByDescending<EntryMatch> { it.exactBarcode }.thenByDescending { it.nameHits })
            .take(limit)
    }

    /** Resolve a match + parse into a concrete line preview (unit-scaled price, clamped qty). */
    fun resolve(match: EntryMatch, parsed: ParsedEntry): EntryPreview {
        val unit = match.unit ?: match.candidate.units.firstOrNull()
            ?: EntryUnit(unitId = match.candidate.product.baseUnitId.orEmpty(), name = "", multiplier = 1.0, decimalPlaces = 0, isBase = true)
        val qty = parsed.quantity?.let { clampToDecimals(it, unit.decimalPlaces) }?.takeIf { it > 0.0 } ?: 1.0
        val price = parsed.priceOverride ?: round2(match.candidate.product.sellingPrice * unit.multiplier)
        val disc = parsed.discountPercent ?: 0.0
        return EntryPreview(
            product = match.candidate.product,
            unit = unit,
            quantity = qty,
            unitPrice = price,
            priceOverridden = parsed.priceOverride != null,
            discountPercent = disc,
            amount = round2(price * qty * (1.0 - disc / 100.0)),
        )
    }

    /** Round a quantity to the unit's allowed decimal places (0 decimals → whole numbers). */
    fun clampToDecimals(value: Double, decimalPlaces: Int): Double {
        val f = 10.0.pow(decimalPlaces.coerceIn(0, 6))
        // epsilon guards against binary float dust (2.5005 × 1000 = 2500.4999…)
        return round(value * f + 1e-9) / f
    }
}

internal fun round2(value: Double): Double = floor(abs(value) * 100.0 + 0.5 + 1e-9) / 100.0 * if (value < 0) -1.0 else 1.0
