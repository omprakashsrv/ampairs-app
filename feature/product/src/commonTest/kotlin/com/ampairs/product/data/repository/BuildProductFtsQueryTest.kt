package com.ampairs.product.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [buildProductFtsQuery] — the FTS4 MATCH-string builder behind product search
 * (name / code / description). Covers per-token prefixing, quote escaping, and the degenerate-input
 * → browse (null) fallback.
 */
class BuildProductFtsQueryTest {

    @Test
    fun blankInputFallsBackToBrowse() {
        assertNull(buildProductFtsQuery(""))
        assertNull(buildProductFtsQuery("   "))
    }

    @Test
    fun punctuationOnlyFallsBackToBrowse() {
        assertNull(buildProductFtsQuery("\""))
        assertNull(buildProductFtsQuery("  --  "))
    }

    @Test
    fun textTokensBecomePerTokenPrefixTerms() {
        assertEquals("\"wid\"*", buildProductFtsQuery("wid"))
        assertEquals("\"wid\"* \"blue\"*", buildProductFtsQuery("wid blue"))
        assertEquals("\"wid\"* \"blue\"*", buildProductFtsQuery("  wid   blue  "))
    }

    @Test
    fun codePrefixIsASingleToken() {
        assertEquals("\"PRD-001\"*", buildProductFtsQuery("PRD-001"))
        assertEquals("\"12345\"*", buildProductFtsQuery("12345"))
    }

    @Test
    fun embeddedQuotesAreEscaped() {
        assertEquals("\"a\"\"b\"*", buildProductFtsQuery("a\"b"))
    }
}
