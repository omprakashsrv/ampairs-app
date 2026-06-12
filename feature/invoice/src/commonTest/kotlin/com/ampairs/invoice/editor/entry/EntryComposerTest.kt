package com.ampairs.invoice.editor.entry

import com.ampairs.product.domain.ProductSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavior tests for the fast-entry parser + matcher, mirroring the v2 prototype
 * (composer.jsx: tokenizeEntry / matchProducts / resolveEntry) and the design seed catalog.
 */
class EntryComposerTest {

    private fun product(id: String, name: String, hsn: String, price: Double, code: String = "") =
        ProductSummary(id = id, name = name, code = code, sellingPrice = price, taxCode = hsn)

    private fun units(vararg pairs: Triple<String, Double, Int>) = pairs.mapIndexed { i, (n, m, d) ->
        EntryUnit(unitId = n, name = n, multiplier = m, decimalPlaces = d, isBase = i == 0)
    }

    private val cement = EntryCandidate(
        product(id = "PRD-CEM", name = "UltraTech PPC Cement 50kg", hsn = "2523", price = 385.0, code = "89012345"),
        units(Triple("BAG", 1.0, 0), Triple("PALLET", 30.0, 0)),
    )
    private val oil = EntryCandidate(
        product(id = "PRD-OIL", name = "Fortune Sunflower Oil", hsn = "1512", price = 145.0, code = "89010630140"),
        units(Triple("PCS", 1.0, 0), Triple("CARTON", 12.0, 0)),
    )
    private val tmt = EntryCandidate(
        product(id = "PRD-TMT", name = "TMT Steel Bar 12mm", hsn = "7214", price = 62.5),
        units(Triple("KG", 1.0, 3), Triple("BUNDLE", 60.0, 0)),
    )
    private val parle = EntryCandidate(
        product(id = "PRD-BIS", name = "Parle-G Biscuits 800g", hsn = "1905", price = 90.0, code = "89014563"),
        units(Triple("PCS", 1.0, 0), Triple("BOX", 24.0, 0)),
    )
    private val catalog = listOf(cement, oil, tmt, parle)

    // ── parser ──

    @Test
    fun parser_fullGrammar() {
        val p = EntryParser.parse("parle 5 box 10% @2000")
        assertEquals(5.0, p.quantity)
        assertEquals(10.0, p.discountPercent)
        assertEquals(2000.0, p.priceOverride)
        assertEquals(listOf("parle", "box"), p.words)
    }

    @Test
    fun parser_rupeePriceToken_andFirstNumberOnlyIsQty() {
        val p = EntryParser.parse("cement 3 ₹380.5 2")
        assertEquals(3.0, p.quantity)
        assertEquals(380.5, p.priceOverride)
        // the second bare number is a word, not a second quantity
        assertEquals(listOf("cement", "2"), p.words)
    }

    @Test
    fun parser_barcodeRunStaysAWord() {
        val p = EntryParser.parse("89014563")
        assertNull(p.quantity)
        assertEquals(listOf("89014563"), p.words)
    }

    // ── matcher ──

    @Test
    fun emptyQuery_showsRecentsFirst_thenRest_max5() {
        val m = EntryMatcher.match(EntryParser.parse(""), catalog, recentProductIds = listOf("PRD-BIS"))
        assertEquals("PRD-BIS", m.first().candidate.product.id)
        assertTrue(m.first().isRecent)
        assertEquals(4, m.size)
    }

    @Test
    fun unitWord_selectsUnit_insteadOfFiltering() {
        val m = EntryMatcher.match(EntryParser.parse("parle 5 box 10%"), catalog, emptyList())
        assertEquals(1, m.size)
        assertEquals("BOX", m.single().unit?.name)
    }

    @Test
    fun acceptanceCheck2_parleBoxPreview() {
        val parsed = EntryParser.parse("parle 5 box 10%")
        val match = EntryMatcher.match(parsed, catalog, emptyList()).single()
        val r = EntryMatcher.resolve(match, parsed)
        assertEquals(5.0, r.quantity)
        assertEquals("BOX", r.unit.name)
        assertEquals(2160.0, r.unitPrice)   // 90 × 24
        assertEquals(10.0, r.discountPercent)
        assertEquals(9720.0, r.amount)      // 2160 × 5 × 0.9
        assertTrue(!r.priceOverridden)
    }

    @Test
    fun hsnPrefix_matches() {
        // the first bare number is the quantity, so HSN digits work as a second number token
        val parsed = EntryParser.parse("2 2523")
        assertEquals(2.0, parsed.quantity)
        val m = EntryMatcher.match(parsed, catalog, emptyList())
        assertEquals(listOf("PRD-CEM"), m.map { it.candidate.product.id })
    }

    @Test
    fun barcode_exactMatch_sortsFirst() {
        val parsed = EntryParser.parse("89014563")
        val m = EntryMatcher.match(parsed, catalog, emptyList())
        assertEquals("PRD-BIS", m.first().candidate.product.id)
        assertTrue(m.first().exactBarcode)
        val r = EntryMatcher.resolve(m.first(), parsed)
        assertEquals(1.0, r.quantity)       // scanner path commits at qty 1
        assertEquals("PCS", r.unit.name)
    }

    @Test
    fun barcodeSubstring_matches() {
        val m = EntryMatcher.match(EntryParser.parse("90106301"), catalog, emptyList())
        assertEquals(listOf("PRD-OIL"), m.map { it.candidate.product.id })
        assertTrue(!m.single().exactBarcode)
    }

    @Test
    fun nonMatchingWord_rejectsCandidate() {
        val m = EntryMatcher.match(EntryParser.parse("parle xyz"), catalog, emptyList())
        assertTrue(m.isEmpty())
    }

    @Test
    fun priceOverride_marksOverridden() {
        val parsed = EntryParser.parse("cement 2 @380")
        val r = EntryMatcher.resolve(EntryMatcher.match(parsed, catalog, emptyList()).single(), parsed)
        assertEquals(380.0, r.unitPrice)
        assertTrue(r.priceOverridden)
        assertEquals(760.0, r.amount)
    }

    @Test
    fun quantity_respectsUnitDecimals() {
        // KG allows 3 decimals; BUNDLE allows 0
        val kg = EntryParser.parse("steel 2.5005")
        val rKg = EntryMatcher.resolve(EntryMatcher.match(kg, catalog, emptyList()).single(), kg)
        assertEquals(2.501, rKg.quantity, 1e-9)
        val bundle = EntryParser.parse("steel 2.6 bundle")
        val rB = EntryMatcher.resolve(EntryMatcher.match(bundle, catalog, emptyList()).single(), bundle)
        assertEquals(3.0, rB.quantity, 1e-9)
    }

    @Test
    fun defaultQuantityIsOne() {
        val parsed = EntryParser.parse("cement")
        val r = EntryMatcher.resolve(EntryMatcher.match(parsed, catalog, emptyList()).single(), parsed)
        assertEquals(1.0, r.quantity)
        assertEquals(385.0, r.unitPrice)
    }
}
