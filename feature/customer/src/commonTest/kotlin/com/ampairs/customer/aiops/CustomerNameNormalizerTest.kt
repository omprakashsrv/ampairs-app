package com.ampairs.customer.aiops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the deterministic decision the `customer.name` capability relies on. Pure-function tests — the
 * cheapest, highest-value coverage for the rule that decides whether an auto-fix fires.
 */
class CustomerNameNormalizerTest {

    @Test
    fun `normalize trims edges and collapses internal whitespace runs`() {
        assertEquals("John Doe", CustomerNameNormalizer.normalize("  John   Doe "))
        assertEquals("Blue Widget XL", CustomerNameNormalizer.normalize("Blue  Widget\tXL"))
    }

    @Test
    fun `normalize never changes words, order, or case`() {
        assertEquals("Acme Pvt Ltd", CustomerNameNormalizer.normalize("Acme Pvt Ltd"))
        assertEquals("aB cD", CustomerNameNormalizer.normalize("aB  cD"))
    }

    @Test
    fun `needsNormalization is true for padded or double-spaced names`() {
        assertTrue(CustomerNameNormalizer.needsNormalization(" John"))
        assertTrue(CustomerNameNormalizer.needsNormalization("John "))
        assertTrue(CustomerNameNormalizer.needsNormalization("John   Doe"))
    }

    @Test
    fun `needsNormalization is false for already-tidy names`() {
        assertFalse(CustomerNameNormalizer.needsNormalization("John Doe"))
        assertFalse(CustomerNameNormalizer.needsNormalization("Acme"))
    }

    @Test
    fun `needsNormalization is false for blank or null`() {
        assertFalse(CustomerNameNormalizer.needsNormalization(null))
        assertFalse(CustomerNameNormalizer.needsNormalization(""))
        assertFalse(CustomerNameNormalizer.needsNormalization("   "))
    }

    @Test
    fun `a normalized value never needs a second pass (idempotent)`() {
        val once = CustomerNameNormalizer.normalize("  Mary  Jane   Watson ")
        assertFalse(CustomerNameNormalizer.needsNormalization(once))
    }
}
