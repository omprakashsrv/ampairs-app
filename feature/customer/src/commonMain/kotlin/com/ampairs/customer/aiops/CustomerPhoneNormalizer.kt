package com.ampairs.customer.aiops

/**
 * Deterministic phone-number normalization used by the AI Ops `customer.phone` capability. Phone numbers
 * are dialled by their digits alone — spaces, dashes, dots, and parentheses are display formatting, not
 * data — so collapsing them to a digits-only canonical form (preserving a leading `+` country prefix) is a
 * safe, reversible data-quality fix. No LLM, HIGH confidence.
 *
 * Pure functions only (KMP-safe); the capability and its tests both use these.
 */
object CustomerPhoneNormalizer {

    /**
     * Canonical storage form: surrounding whitespace removed, every non-digit stripped, and a single
     * leading `+` (international prefix) preserved when the trimmed input started with one.
     */
    fun normalize(phone: String): String {
        val trimmed = phone.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }

    /**
     * True when [phone] carries at least one digit and isn't already in canonical form — i.e. there's a
     * real, safe normalization to apply. Blank/null, or text whose canonical form has no digits at all
     * (e.g. `"n/a"`), → false.
     */
    fun needsNormalization(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val canonical = normalize(phone)
        if (canonical.none { it.isDigit() }) return false
        return phone != canonical
    }
}
