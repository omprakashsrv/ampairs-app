package com.ampairs.unit.aiops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the deterministic alias reference data the unit-standardization capability relies on. These are
 * pure-function tests (no DB, no DI) — the highest-value, cheapest coverage for the rule that decides
 * whether an auto-fix even fires.
 */
class UnitAliasCatalogTest {

    @Test
    fun `normalize trims uppercases and strips trailing dot and collapses spaces`() {
        assertEquals("KG", UnitAliasCatalog.normalize("  kg "))
        assertEquals("NOS", UnitAliasCatalog.normalize("nos."))
        assertEquals("KILO GRAM", UnitAliasCatalog.normalize("kilo   gram"))
    }

    @Test
    fun `known weight aliases resolve to KG`() {
        for (alias in listOf("Kgs", "kg", "Kilo", "kilos", "Kilogram", "KILOGRAMS")) {
            assertEquals("KG", UnitAliasCatalog.canonicalFor(alias), "\"$alias\" should map to KG")
        }
    }

    @Test
    fun `known volume aliases resolve to L and ML`() {
        for (alias in listOf("Ltr", "litres", "LITER", "l")) {
            assertEquals("L", UnitAliasCatalog.canonicalFor(alias))
        }
        for (alias in listOf("ml", "Millilitre", "milliliters")) {
            assertEquals("ML", UnitAliasCatalog.canonicalFor(alias))
        }
    }

    @Test
    fun `a canonical spelling maps to itself`() {
        assertEquals("KG", UnitAliasCatalog.canonicalFor("KG"))
        assertEquals("PCS", UnitAliasCatalog.canonicalFor("PCS"))
    }

    @Test
    fun `an unknown spelling has no canonical`() {
        assertNull(UnitAliasCatalog.canonicalFor("widget"))
        assertNull(UnitAliasCatalog.canonicalFor(""))
        assertNull(UnitAliasCatalog.canonicalFor("   "))
    }

    @Test
    fun `every alias target is itself a declared canonical`() {
        // Guards against a typo mapping to a value that isn't in the canonical set.
        for (alias in listOf("Kgs", "gram", "ltr", "ml", "pcs", "dozen", "boxes", "metre", "cm", "mm")) {
            val canonical = UnitAliasCatalog.canonicalFor(alias)
            assertTrue(
                canonical != null && canonical in UnitAliasCatalog.canonicals,
                "\"$alias\" → $canonical must be a declared canonical",
            )
        }
    }
}
