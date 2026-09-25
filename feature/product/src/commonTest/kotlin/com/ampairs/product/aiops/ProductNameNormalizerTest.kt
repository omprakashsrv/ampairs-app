package com.ampairs.product.aiops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the deterministic decision the `product.name` capability relies on. Pure-function tests — the
 * cheapest, highest-value coverage for the rule that decides whether an auto-fix fires.
 */
class ProductNameNormalizerTest {

    @Test
    fun `normalize trims edges and collapses internal whitespace runs`() {
        assertEquals("Blue Widget XL", ProductNameNormalizer.normalize("  Blue   Widget XL "))
        assertEquals("Steel Bolt M8", ProductNameNormalizer.normalize("Steel  Bolt\tM8"))
    }

    @Test
    fun `normalize never changes words, order, or case`() {
        assertEquals("Acme Pro 2000", ProductNameNormalizer.normalize("Acme Pro 2000"))
        assertEquals("aB cD", ProductNameNormalizer.normalize("aB  cD"))
    }

    @Test
    fun `needsNormalization is true for padded or double-spaced names`() {
        assertTrue(ProductNameNormalizer.needsNormalization(" Widget"))
        assertTrue(ProductNameNormalizer.needsNormalization("Widget "))
        assertTrue(ProductNameNormalizer.needsNormalization("Blue  Widget"))
    }

    @Test
    fun `needsNormalization is false for already-tidy names`() {
        assertFalse(ProductNameNormalizer.needsNormalization("Blue Widget"))
        assertFalse(ProductNameNormalizer.needsNormalization("Widget"))
    }

    @Test
    fun `needsNormalization is false for blank or null`() {
        assertFalse(ProductNameNormalizer.needsNormalization(null))
        assertFalse(ProductNameNormalizer.needsNormalization(""))
        assertFalse(ProductNameNormalizer.needsNormalization("   "))
    }

    @Test
    fun `a normalized value never needs a second pass (idempotent)`() {
        val once = ProductNameNormalizer.normalize("  Big   Red  Box ")
        assertFalse(ProductNameNormalizer.needsNormalization(once))
    }
}
