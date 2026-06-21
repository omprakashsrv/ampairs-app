package com.ampairs.printing.core.model

/**
 * A resolved, typed field value placed into the IR. Locale-specific formatting (currency symbol,
 * grouping, date pattern, timezone) is applied by a [ValueFormatter] at render time — NOT here —
 * so `printing/core` stays free of locale concerns and totals are never recomputed (FR-005, §20).
 */
sealed interface FieldValue {
    data class Text(val value: String) : FieldValue
    data class Number(val value: Double, val decimals: Int = 2) : FieldValue
    /** A monetary amount; the renderer applies the workspace currency symbol + grouping. */
    data class Money(val amount: Double) : FieldValue
    /** A UTC instant in epoch millis; the renderer applies the business timezone + date format. */
    data class DateValue(val epochMillis: Long) : FieldValue
    data class Bool(val value: Boolean) : FieldValue
    data object Empty : FieldValue
}

/** Turns a typed [FieldValue] into display text. Renderers pass a locale-aware implementation. */
fun interface ValueFormatter {
    fun format(value: FieldValue): String
}

/**
 * Locale-free fallback formatter (tests, raw output). Money/date render without symbol/zone — a
 * real renderer supplies a locale-aware [ValueFormatter] from the feature layer.
 */
object PlainValueFormatter : ValueFormatter {
    override fun format(value: FieldValue): String = when (value) {
        is FieldValue.Text -> value.value
        is FieldValue.Number -> trimDecimals(value.value, value.decimals)
        is FieldValue.Money -> trimDecimals(value.amount, 2)
        is FieldValue.DateValue -> value.epochMillis.toString()
        is FieldValue.Bool -> if (value.value) "Yes" else "No"
        FieldValue.Empty -> ""
    }

    private fun trimDecimals(value: Double, decimals: Int): String {
        if (decimals <= 0) return value.toLong().toString()
        val factor = generateSequence(1L) { it * 10 }.elementAt(decimals)
        val rounded = kotlin.math.round(value * factor) / factor
        val whole = rounded.toLong()
        val frac = kotlin.math.round((kotlin.math.abs(rounded) - kotlin.math.abs(whole)) * factor).toLong()
        return "$whole.${frac.toString().padStart(decimals, '0')}"
    }
}
