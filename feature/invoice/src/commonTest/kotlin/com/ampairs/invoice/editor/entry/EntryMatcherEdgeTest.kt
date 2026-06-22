package com.ampairs.invoice.editor.entry

import com.ampairs.product.domain.ProductSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Edge-case + numeric tests for the fast-entry matcher/resolver that the behaviour-focused
 * [EntryComposerTest] does not cover: empty catalogs, multi-word name ranking, the recents cap,
 * the unit-decimal clamp boundaries, and the [round2] half-up rounding rule. Pure functions only.
 */
class EntryMatcherEdgeTest {

    private fun product(id: String, name: String, hsn: String = "", price: Double = 100.0, code: String = "") =
        ProductSummary(id = id, name = name, code = code, sellingPrice = price, taxCode = hsn)

    private fun candidate(
        id: String,
        name: String,
        hsn: String = "",
        price: Double = 100.0,
        code: String = "",
        units: List<EntryUnit> = listOf(baseUnit()),
    ) = EntryCandidate(product(id, name, hsn, price, code), units)

    private fun baseUnit(name: String = "PCS", multiplier: Double = 1.0, decimals: Int = 0) =
        EntryUnit(unitId = name, name = name, multiplier = multiplier, decimalPlaces = decimals, isBase = true)

    // ── matcher edge cases ──

    @Test
    fun emptyCatalog_yieldsNoMatches() {
        val m = EntryMatcher.match(EntryParser.parse("anything"), emptyList(), emptyList())
        assertTrue(m.isEmpty())
    }

    @Test
    fun emptyQuery_emptyCatalog_isEmpty() {
        val m = EntryMatcher.match(EntryParser.parse(""), emptyList(), recentProductIds = listOf("X"))
        assertTrue(m.isEmpty())
    }

    @Test
    fun multiWordQuery_requiresEveryWordToMatchTheName() {
        val catalog = listOf(
            candidate("A", "Red Cotton Shirt"),
            candidate("B", "Red Wool Cap"),
        )
        val m = EntryMatcher.match(EntryParser.parse("red shirt"), catalog, emptyList())
        assertEquals(listOf("A"), m.map { it.candidate.product.id }, "both words must hit the name")
    }

    @Test
    fun moreNameHits_rankHigher() {
        val catalog = listOf(
            candidate("ONE", "alpha solo"),
            candidate("TWO", "alpha beta combo"),
        )
        // both words hit TWO (alpha, beta); only "alpha" hits ONE → ONE rejected (beta missing)
        val m = EntryMatcher.match(EntryParser.parse("alpha beta"), catalog, emptyList())
        assertEquals(listOf("TWO"), m.map { it.candidate.product.id })
        assertEquals(2, m.single().nameHits)
    }

    @Test
    fun resultsAreCappedAtMaxResults() {
        val catalog = (1..10).map { candidate("P$it", "common item $it") }
        val m = EntryMatcher.match(EntryParser.parse("common"), catalog, emptyList())
        assertEquals(EntryMatcher.MAX_RESULTS, m.size)
    }

    @Test
    fun emptyQuery_recentsCapAtLimit_andMarkedRecent() {
        val catalog = (1..10).map { candidate("P$it", "item $it") }
        val recents = listOf("P9", "P8")
        val m = EntryMatcher.match(EntryParser.parse(""), catalog, recents)
        assertEquals(EntryMatcher.MAX_RESULTS, m.size)
        assertEquals(listOf("P9", "P8"), m.take(2).map { it.candidate.product.id }, "recents come first, in order")
        assertTrue(m[0].isRecent)
        assertTrue(m[1].isRecent)
        assertFalse(m[2].isRecent)
    }

    @Test
    fun hsnMatchesOnPrefixNotSuffix() {
        val catalog = listOf(candidate("A", "thing", hsn = "2523"))
        // A bare 1–7 digit number is parsed as the *quantity*, not a search word; the leading "1 "
        // consumes the quantity slot so the second numeric token is kept as a word to match on.
        // HSN matching is prefix-only (hsn.startsWith(word)):
        //   "252" is a prefix of "2523" → match
        assertEquals(1, EntryMatcher.match(EntryParser.parse("1 252"), catalog, emptyList()).size)
        //   "523" is a suffix, not a prefix → no match
        assertTrue(EntryMatcher.match(EntryParser.parse("1 523"), catalog, emptyList()).isEmpty())
    }

    // ── resolve: unit fallback + decimals ──

    @Test
    fun resolve_withNoUnits_usesBaseUnitPseudoFallback() {
        val cand = EntryCandidate(product("A", "thing", price = 42.0), units = emptyList())
        val parsed = EntryParser.parse("thing")
        val match = EntryMatcher.match(parsed, listOf(cand), emptyList()).single()
        val r = EntryMatcher.resolve(match, parsed)
        assertEquals(1.0, r.quantity)
        assertEquals(42.0, r.unitPrice, "1× pseudo-unit keeps the product price")
    }

    @Test
    fun resolve_zeroOrNegativeQuantity_clampsToOne() {
        val cand = candidate("A", "thing", price = 10.0, units = listOf(baseUnit(decimals = 0)))
        // "0" parses as quantity 0 → clamped, then takeIf{>0} fails → default 1.0
        val parsed = EntryParser.parse("thing 0")
        val r = EntryMatcher.resolve(EntryMatcher.match(parsed, listOf(cand), emptyList()).single(), parsed)
        assertEquals(1.0, r.quantity)
    }

    @Test
    fun clampToDecimals_boundaries() {
        assertEquals(3.0, EntryMatcher.clampToDecimals(2.5, 0), 1e-9)
        assertEquals(2.0, EntryMatcher.clampToDecimals(2.4, 0), 1e-9)
        assertEquals(2.501, EntryMatcher.clampToDecimals(2.5005, 3), 1e-9)
        assertEquals(2.5, EntryMatcher.clampToDecimals(2.5, 6), 1e-9)
        // negative decimalPlaces is coerced to 0
        assertEquals(3.0, EntryMatcher.clampToDecimals(2.7, -5), 1e-9)
    }

    // ── round2 ──

    @Test
    fun round2_halfUp_positive() {
        assertEquals(2.13, round2(2.125), 1e-9)
        assertEquals(2.13, round2(2.126), 1e-9)
        assertEquals(0.0, round2(0.0), 1e-9)
        assertEquals(9720.0, round2(2160.0 * 5 * 0.9), 1e-9)
    }

    @Test
    fun round2_preservesSignForNegatives() {
        assertEquals(-2.13, round2(-2.125), 1e-9)
        assertEquals(-1.0, round2(-1.0), 1e-9)
    }
}
