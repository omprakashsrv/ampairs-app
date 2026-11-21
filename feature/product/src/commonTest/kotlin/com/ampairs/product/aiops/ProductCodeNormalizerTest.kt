package com.ampairs.product.aiops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the deterministic decision the `product.code` capability relies on. Pure-function tests — the
 * cheapest, highest-value coverage for the rule that decides whether an auto-fix fires.
 */
class ProductCodeNormalizerTest {

    @Test
    fun `normalize trims and upper-cases`() {
        assertEquals("SKU-001", ProductCodeNormalizer.normalize("  sku-001 "))
        assertEquals("AB12", ProductCodeNormalizer.normalize("aB12"))
    }

    @Test
    fun `needsNormalization is true for lower or mixed case or padded codes`() {
        assertTrue(ProductCodeNormalizer.needsNormalization("sku-001"))
        assertTrue(ProductCodeNormalizer.needsNormalization("Sku-001"))
        assertTrue(ProductCodeNormalizer.needsNormalization(" ABC "))
    }

    @Test
    fun `needsNormalization is false for already-canonical codes`() {
        assertFalse(ProductCodeNormalizer.needsNormalization("SKU-001"))
        assertFalse(ProductCodeNormalizer.needsNormalization("12345"))
    }

    @Test
    fun `needsNormalization is false for blank or null`() {
        assertFalse(ProductCodeNormalizer.needsNormalization(null))
        assertFalse(ProductCodeNormalizer.needsNormalization(""))
        assertFalse(ProductCodeNormalizer.needsNormalization("   "))
    }

    @Test
    fun `a normalized value never needs a second pass (idempotent)`() {
        val once = ProductCodeNormalizer.normalize("  mixed-Code-9 ")
        assertFalse(ProductCodeNormalizer.needsNormalization(once))
    }
}
