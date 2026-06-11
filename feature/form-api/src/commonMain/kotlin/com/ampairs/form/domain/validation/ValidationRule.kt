package com.ampairs.form.domain.validation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed validation rule (mirrors the backend `ValidationRule`). Only fields relevant to [type] are set.
 *
 * NOTE (spec 011): drafted, not yet compile-gated.
 */
@Serializable
data class ValidationRule(
    val type: ValidationRuleType,
    val min: Int? = null,
    val max: Int? = null,
    @SerialName("min_value") val minValue: Double? = null,
    @SerialName("max_value") val maxValue: Double? = null,
    val kind: FormatKind? = null,
    val values: List<String>? = null,
)

@Serializable
enum class ValidationRuleType {
    @SerialName("required") REQUIRED,
    @SerialName("length_range") LENGTH_RANGE,
    @SerialName("number_range") NUMBER_RANGE,
    @SerialName("format") FORMAT,
    @SerialName("allowed_choices") ALLOWED_CHOICES,
}

@Serializable
enum class FormatKind {
    @SerialName("email") EMAIL,
    @SerialName("phone") PHONE,
    @SerialName("gstin") GSTIN,
    @SerialName("pan") PAN,
    @SerialName("url") URL,
    @SerialName("numeric") NUMERIC,
}
