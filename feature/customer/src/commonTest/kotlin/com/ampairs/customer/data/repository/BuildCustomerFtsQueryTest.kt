package com.ampairs.customer.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [buildCustomerFtsQuery] — the FTS4 MATCH-string builder behind customer search
 * (spec 001, quickstart §6). Covers per-token prefixing, phone digit-collapse, quote escaping,
 * and the degenerate-input → browse (null) fallback.
 */
class BuildCustomerFtsQueryTest {

    @Test
    fun blankInputFallsBackToBrowse() {
        assertNull(buildCustomerFtsQuery(""))
        assertNull(buildCustomerFtsQuery("   "))
    }

    @Test
    fun punctuationOnlyFallsBackToBrowse() {
        // No alphanumerics → nothing to match → browse path.
        assertNull(buildCustomerFtsQuery("\""))
        assertNull(buildCustomerFtsQuery("  -–—  "))
    }

    @Test
    fun textTokensBecomePerTokenPrefixTerms() {
        assertEquals("\"raj\"*", buildCustomerFtsQuery("raj"))
        assertEquals("\"raj\"* \"mum\"*", buildCustomerFtsQuery("raj mum"))
        assertEquals("\"raj\"* \"mum\"*", buildCustomerFtsQuery("  raj   mum  "))
    }

    @Test
    fun gstinPrefixIsASingleToken() {
        assertEquals("\"29ABCDE\"*", buildCustomerFtsQuery("29ABCDE"))
    }

    @Test
    fun phoneDigitsCollapseToOnePrefixToken() {
        assertEquals("\"9876543210\"*", buildCustomerFtsQuery("9876543210"))
        // Spaced phone → single token so it matches the stored single-token phone.
        assertEquals("\"9876543210\"*", buildCustomerFtsQuery("98765 43210"))
    }

    @Test
    fun embeddedQuotesAreEscaped() {
        // a"b → quote doubled inside the quoted term: "a""b"*
        assertEquals("\"a\"\"b\"*", buildCustomerFtsQuery("a\"b"))
    }
}
