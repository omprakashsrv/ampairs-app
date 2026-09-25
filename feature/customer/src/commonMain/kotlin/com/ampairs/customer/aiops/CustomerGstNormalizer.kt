package com.ampairs.customer.aiops

/**
 * Deterministic GSTIN normalization used by the AI Ops `customer.gst` capability. A GSTIN is a
 * regulated 15-character identifier whose canonical form is upper-case with no surrounding whitespace —
 * it's matched and validated exactly — so trimming and upper-casing is a safe, reversible data-quality
 * fix (users routinely paste it lower-cased or padded). No LLM, HIGH confidence.
 *
 * Pure functions only (KMP-safe); the capability and its tests both use these. Note: only edge whitespace
 * and letter case are touched — internal characters are never removed, so the value can always be reversed.
 */
object CustomerGstNormalizer {

    /** Canonical storage form: surrounding whitespace removed and upper-cased. */
    fun normalize(gst: String): String = gst.trim().uppercase()

    /**
     * True when [gst] is a non-blank value that isn't already in canonical form — i.e. there's a real,
     * safe normalization to apply (lower/mixed-case or padded). Blank/null → false.
     */
    fun needsNormalization(gst: String?): Boolean {
        if (gst.isNullOrBlank()) return false
        return gst != normalize(gst)
    }
}
