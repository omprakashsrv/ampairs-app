package com.ampairs.customer.aiops

/**
 * Deterministic email normalization used by the AI Ops `customer.email` capability. Email addresses are
 * case-insensitive in practice (the domain part always, the local part for effectively every provider),
 * so lowercasing + trimming is a safe, reversible data-quality fix — no LLM, HIGH confidence.
 *
 * Pure functions only (KMP-safe); the capability and its tests both use these.
 */
object CustomerEmailNormalizer {

    /** Canonical storage form: surrounding whitespace removed and lowercased. */
    fun normalize(email: String): String = email.trim().lowercase()

    /**
     * True when [email] is a plausible address (contains `@`) that isn't already in canonical form —
     * i.e. there's a real, safe normalization to apply. Blank/null or non-email text → false.
     */
    fun needsNormalization(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        if (!email.contains('@')) return false
        return email != normalize(email)
    }
}
