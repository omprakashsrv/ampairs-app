package com.ampairs.customer.aiops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the deterministic decision the `customer.phone` capability relies on. Pure-function tests — the
 * cheapest, highest-value coverage for the rule that decides whether an auto-fix fires.
 */
class CustomerPhoneNormalizerTest {

    @Test
    fun `normalize strips spaces, dashes, dots, and parentheses`() {
        assertEquals("2345678900", CustomerPhoneNormalizer.normalize("234-567.8900"))
        assertEquals("2345678900", CustomerPhoneNormalizer.normalize(" (234) 567 8900 "))
    }

    @Test
    fun `normalize preserves a single leading plus prefix`() {
        assertEquals("+12345678900", CustomerPhoneNormalizer.normalize("+1 (234) 567-8900"))
        assertEquals("+919876543210", CustomerPhoneNormalizer.normalize("+91 98765 43210"))
    }

    @Test
    fun `normalize drops a non-leading plus`() {
        assertEquals("12345", CustomerPhoneNormalizer.normalize("1+2345"))
    }

    @Test
    fun `needsNormalization is true for formatted numbers`() {
        assertTrue(CustomerPhoneNormalizer.needsNormalization("234-567-8900"))
        assertTrue(CustomerPhoneNormalizer.needsNormalization(" 9876543210"))
        assertTrue(CustomerPhoneNormalizer.needsNormalization("+91 98765 43210"))
    }

    @Test
    fun `needsNormalization is false for already-canonical numbers`() {
        assertFalse(CustomerPhoneNormalizer.needsNormalization("2345678900"))
        assertFalse(CustomerPhoneNormalizer.needsNormalization("+12345678900"))
    }

    @Test
    fun `needsNormalization is false for blank, null, or digitless text`() {
        assertFalse(CustomerPhoneNormalizer.needsNormalization(null))
        assertFalse(CustomerPhoneNormalizer.needsNormalization(""))
        assertFalse(CustomerPhoneNormalizer.needsNormalization("   "))
        assertFalse(CustomerPhoneNormalizer.needsNormalization("n/a"))
    }

    @Test
    fun `a normalized value never needs a second pass (idempotent)`() {
        val once = CustomerPhoneNormalizer.normalize("+1 (234) 567-8900")
        assertFalse(CustomerPhoneNormalizer.needsNormalization(once))
    }
}
