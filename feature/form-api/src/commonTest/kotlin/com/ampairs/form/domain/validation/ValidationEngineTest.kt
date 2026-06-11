package com.ampairs.form.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Renderer-side validation rules (spec 011, T037 subset): required, length/number ranges,
 * format kinds, allowed choices — the engine the schema-driven form enforces inline at entry.
 */
class ValidationEngineTest {

    private fun rule(
        type: ValidationRuleType,
        min: Int? = null,
        max: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        kind: FormatKind? = null,
        values: List<String>? = null,
    ) = ValidationRule(type = type, min = min, max = max, minValue = minValue, maxValue = maxValue, kind = kind, values = values)

    @Test
    fun required_blocks_empty_and_passes_filled() {
        val rules = listOf(rule(ValidationRuleType.REQUIRED))
        assertNotNull(ValidationEngine.firstError("", rules, isEmpty = true))
        assertNull(ValidationEngine.firstError("x", rules, isEmpty = false))
    }

    @Test
    fun null_or_no_rules_always_pass() {
        assertNull(ValidationEngine.firstError("anything", null, isEmpty = false))
        assertNull(ValidationEngine.firstError("", emptyList(), isEmpty = true))
    }

    @Test
    fun length_range_enforced_only_when_not_empty() {
        val rules = listOf(rule(ValidationRuleType.LENGTH_RANGE, min = 3, max = 5))
        assertNull(ValidationEngine.firstError("", rules, isEmpty = true))      // empty: not REQUIRED's job
        assertNotNull(ValidationEngine.firstError("ab", rules, isEmpty = false))
        assertNull(ValidationEngine.firstError("abc", rules, isEmpty = false))
        assertNull(ValidationEngine.firstError("abcde", rules, isEmpty = false))
        assertNotNull(ValidationEngine.firstError("abcdef", rules, isEmpty = false))
    }

    @Test
    fun number_range_rejects_non_numeric_and_out_of_range() {
        val rules = listOf(rule(ValidationRuleType.NUMBER_RANGE, minValue = 1.0, maxValue = 10.0))
        assertEquals("Must be a number", ValidationEngine.firstError("abc", rules, isEmpty = false))
        assertNotNull(ValidationEngine.firstError("0.5", rules, isEmpty = false))
        assertNull(ValidationEngine.firstError("5", rules, isEmpty = false))
        assertNotNull(ValidationEngine.firstError("11", rules, isEmpty = false))
    }

    @Test
    fun email_format() {
        val rules = listOf(rule(ValidationRuleType.FORMAT, kind = FormatKind.EMAIL))
        assertNull(ValidationEngine.firstError("a@b.co", rules, isEmpty = false))
        assertNotNull(ValidationEngine.firstError("not-an-email", rules, isEmpty = false))
        assertNull(ValidationEngine.firstError("", rules, isEmpty = true))
    }

    @Test
    fun gstin_and_pan_formats() {
        val gst = listOf(rule(ValidationRuleType.FORMAT, kind = FormatKind.GSTIN))
        assertNull(ValidationEngine.firstError("29ABCDE1234F1Z5", gst, isEmpty = false))
        assertNotNull(ValidationEngine.firstError("INVALID", gst, isEmpty = false))

        val pan = listOf(rule(ValidationRuleType.FORMAT, kind = FormatKind.PAN))
        assertNull(ValidationEngine.firstError("ABCDE1234F", pan, isEmpty = false))
        assertNotNull(ValidationEngine.firstError("1234ABCDE", pan, isEmpty = false))
    }

    @Test
    fun allowed_choices_restricts_values() {
        val rules = listOf(rule(ValidationRuleType.ALLOWED_CHOICES, values = listOf("A", "B")))
        assertNull(ValidationEngine.firstError("A", rules, isEmpty = false))
        assertNotNull(ValidationEngine.firstError("C", rules, isEmpty = false))
    }

    @Test
    fun first_failing_rule_wins() {
        val rules = listOf(
            rule(ValidationRuleType.REQUIRED),
            rule(ValidationRuleType.LENGTH_RANGE, min = 5),
        )
        assertEquals("This field is required", ValidationEngine.firstError("", rules, isEmpty = true))
        assertEquals("Must be at least 5 characters", ValidationEngine.firstError("ab", rules, isEmpty = false))
    }
}
