package com.ampairs.product.aiops

/**
 * Deterministic whitespace normalization for the AI Ops `product.name` capability. Product names are
 * frequently pasted or typed with stray whitespace — leading/trailing padding, or runs of spaces/tabs
 * between words ("Blue  Widget\tXL" → "Blue Widget XL"). Collapsing internal whitespace runs to a single
 * space and trimming the edges never changes the words, their order, or their case, only the spacing, and
 * the original is preserved in the audit trail — so it's a safe, reversible fix.
 *
 * Pure functions only (KMP-safe); the capability and its tests both use these.
 */
object ProductNameNormalizer {

    private val whitespaceRun = Regex("\\s+")

    /** Canonical form: edges trimmed and every internal run of whitespace collapsed to one space. */
    fun normalize(name: String): String = name.trim().replace(whitespaceRun, " ")

    /**
     * True when [name] is non-blank and carries stray whitespace (edge padding or an internal run) — i.e.
     * there's a real, safe normalization to apply. Blank/null → false.
     */
    fun needsNormalization(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return name != normalize(name)
    }
}
