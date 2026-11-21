package com.ampairs.customer.aiops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the deterministic decision the `customer.gst` capability relies on. Pure-function tests — the
 * cheapest, highest-value coverage for the rule that decides whether an auto-fix fires.
 */
class CustomerGstNormalizerTest {

    @Test
    fun `normalize trims and upper-cases`() {
        assertEquals("22AAAAA0000A1Z5", CustomerGstNormalizer.normalize("  22aaaaa0000a1z5 "))
        assertEquals("29ABCDE1234F2Z6", CustomerGstNormalizer.normalize("29abcde1234f2z6"))
    }

    @Test
    fun `needsNormalization is true for lower or mixed case or padded gstins`() {
        assertTrue(CustomerGstNormalizer.needsNormalization("22aaaaa0000a1z5"))
        assertTrue(CustomerGstNormalizer.needsNormalization("22Aaaaa0000a1Z5"))
        assertTrue(CustomerGstNormalizer.needsNormalization(" 22AAAAA0000A1Z5 "))
    }

    @Test
    fun `needsNormalization is false for already-canonical gstins`() {
        assertFalse(CustomerGstNormalizer.needsNormalization("22AAAAA0000A1Z5"))
    }

    @Test
    fun `needsNormalization is false for blank or null`() {
        assertFalse(CustomerGstNormalizer.needsNormalization(null))
        assertFalse(CustomerGstNormalizer.needsNormalization(""))
        assertFalse(CustomerGstNormalizer.needsNormalization("   "))
    }

    @Test
    fun `a normalized value never needs a second pass (idempotent)`() {
        val once = CustomerGstNormalizer.normalize("  27abcde9999z1Z0 ")
        assertFalse(CustomerGstNormalizer.needsNormalization(once))
    }
}
