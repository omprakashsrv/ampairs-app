package com.ampairs.product.aiops

/**
 * Deterministic product-code (SKU) normalization used by the AI Ops `product.code` capability. A product
 * code is a machine identifier — looked up, scanned, and matched exactly — so surrounding whitespace and
 * inconsistent letter case are data-entry noise, not meaning. Trimming and upper-casing to a canonical
 * form is a safe, reversible data-quality fix. No LLM, HIGH confidence.
 *
 * Pure functions only (KMP-safe); the capability and its tests both use these.
 */
object ProductCodeNormalizer {

    /** Canonical storage form: surrounding whitespace removed and upper-cased. */
    fun normalize(code: String): String = code.trim().uppercase()

    /**
     * True when [code] is a non-blank identifier that isn't already in canonical form — i.e. there's a
     * real, safe normalization to apply (padded or lower/mixed-case). Blank/null → false.
     */
    fun needsNormalization(code: String?): Boolean {
        if (code.isNullOrBlank()) return false
        return code != normalize(code)
    }
}
