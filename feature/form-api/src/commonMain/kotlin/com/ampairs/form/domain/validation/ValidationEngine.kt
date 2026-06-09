package com.ampairs.form.domain.validation

/**
 * Evaluates a field's value against its typed [ValidationRule]s. Returns the first failing rule's
 * message, or null when valid. Shared by the renderer (inline at entry). The same rule vocabulary is
 * enforced server-side. Messages are intentionally plain (the renderer may map to localized strings).
 *
 * NOTE (spec 011): drafted, not yet compile-gated.
 */
object ValidationEngine {

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE = Regex("^[0-9+\\-() ]{6,20}$")
    private val GSTIN = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
    private val PAN = Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")
    private val NUMERIC = Regex("^-?[0-9]*\\.?[0-9]+$")

    /**
     * @param value the raw entered value (a single value, or for multi-choice the joined/each value)
     * @param rules the field's rules
     * @param isEmpty whether the field currently has no value (drives REQUIRED)
     * @return the first violation message, or null if all rules pass
     */
    fun firstError(value: String, rules: List<ValidationRule>?, isEmpty: Boolean): String? {
        if (rules == null) return null
        for (rule in rules) {
            val error = when (rule.type) {
                ValidationRuleType.REQUIRED -> if (isEmpty) "This field is required" else null
                ValidationRuleType.LENGTH_RANGE -> when {
                    isEmpty -> null
                    rule.min != null && value.length < rule.min -> "Must be at least ${rule.min} characters"
                    rule.max != null && value.length > rule.max -> "Must be at most ${rule.max} characters"
                    else -> null
                }
                ValidationRuleType.NUMBER_RANGE -> {
                    val num = value.toDoubleOrNull()
                    when {
                        isEmpty -> null
                        num == null -> "Must be a number"
                        rule.minValue != null && num < rule.minValue -> "Must be ≥ ${rule.minValue}"
                        rule.maxValue != null && num > rule.maxValue -> "Must be ≤ ${rule.maxValue}"
                        else -> null
                    }
                }
                ValidationRuleType.FORMAT -> if (isEmpty) null else formatError(value, rule.kind)
                ValidationRuleType.ALLOWED_CHOICES -> when {
                    isEmpty -> null
                    rule.values != null && value !in rule.values -> "Not an allowed value"
                    else -> null
                }
            }
            if (error != null) return error
        }
        return null
    }

    private fun formatError(value: String, kind: FormatKind?): String? = when (kind) {
        FormatKind.EMAIL -> if (EMAIL.matches(value)) null else "Enter a valid email"
        FormatKind.PHONE -> if (PHONE.matches(value)) null else "Enter a valid phone number"
        FormatKind.GSTIN -> if (GSTIN.matches(value)) null else "Enter a valid GSTIN"
        FormatKind.PAN -> if (PAN.matches(value)) null else "Enter a valid PAN"
        FormatKind.URL -> if (value.startsWith("http://") || value.startsWith("https://")) null else "Enter a valid URL"
        FormatKind.NUMERIC -> if (NUMERIC.matches(value)) null else "Must be numeric"
        null -> null
    }
}
