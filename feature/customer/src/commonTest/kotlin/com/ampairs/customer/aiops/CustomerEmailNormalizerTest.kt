package com.ampairs.customer.aiops

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the deterministic decision the `customer.email` capability relies on. Pure-function tests — the
 * cheapest, highest-value coverage for the rule that decides whether an auto-fix fires.
 */
class CustomerEmailNormalizerTest {

    @Test
    fun `normalize trims and lowercases`() {
        assertEquals("jo@x.com", CustomerEmailNormalizer.normalize("  Jo@X.CoM "))
        assertEquals("a@b.io", CustomerEmailNormalizer.normalize("A@B.io"))
    }

    @Test
    fun `needsNormalization is true for mixed-case or padded emails`() {
        assertTrue(CustomerEmailNormalizer.needsNormalization("Jo@X.com"))
        assertTrue(CustomerEmailNormalizer.needsNormalization(" jo@x.com"))
        assertTrue(CustomerEmailNormalizer.needsNormalization("jo@x.com "))
    }

    @Test
    fun `needsNormalization is false for already-canonical emails`() {
        assertFalse(CustomerEmailNormalizer.needsNormalization("jo@x.com"))
    }

    @Test
    fun `needsNormalization is false for blank, null, or non-email text`() {
        assertFalse(CustomerEmailNormalizer.needsNormalization(null))
        assertFalse(CustomerEmailNormalizer.needsNormalization(""))
        assertFalse(CustomerEmailNormalizer.needsNormalization("   "))
        assertFalse(CustomerEmailNormalizer.needsNormalization("NotAnEmail"))
    }

    @Test
    fun `a normalized value never needs a second pass (idempotent)`() {
        val once = CustomerEmailNormalizer.normalize("  MiXeD@Case.COM ")
        assertFalse(CustomerEmailNormalizer.needsNormalization(once))
    }
}
