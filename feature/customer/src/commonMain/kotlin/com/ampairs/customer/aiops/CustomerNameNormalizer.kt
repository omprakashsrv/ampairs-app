package com.ampairs.customer.aiops

/**
 * Deterministic whitespace normalization used by the AI Ops `customer.name` capability. Names are
 * frequently pasted or typed with stray whitespace — leading/trailing spaces, or runs of spaces/tabs
 * between words ("John   Doe " → "John Doe"). Collapsing internal whitespace runs to a single space and
 * trimming the edges is a safe fix: it never changes the words, their order, or their case, only the
 * spacing, and the original is preserved in the audit trail so the change is reversible.
 *
 * This is a *different transform kind* from the case/format normalizers (email/phone/gst/product-code) —
 * it introduces internal-whitespace collapse to the capability suite. Pure functions only (KMP-safe).
 */
object CustomerNameNormalizer {

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
