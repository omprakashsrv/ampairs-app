package com.ampairs.form.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.OptionSource

/** Icon + human label for a field's data type (mirrors the design's TYPES map). */
internal fun FieldDataType.icon(): ImageVector = when (this) {
    FieldDataType.TEXT -> Icons.Filled.TextFields
    FieldDataType.TEXTAREA -> Icons.AutoMirrored.Filled.Notes
    FieldDataType.NUMBER -> Icons.Filled.Pin
    FieldDataType.BOOLEAN -> Icons.Filled.ToggleOn
    FieldDataType.DATE -> Icons.Filled.CalendarToday
    FieldDataType.CHOICE -> Icons.Filled.RadioButtonChecked
    FieldDataType.MULTI_CHOICE -> Icons.Filled.Checklist
    FieldDataType.CUSTOM -> Icons.Filled.Widgets
}

internal fun FieldDataType.typeLabel(): String = when (this) {
    FieldDataType.TEXT -> "Text"
    FieldDataType.TEXTAREA -> "Paragraph"
    FieldDataType.NUMBER -> "Number"
    FieldDataType.BOOLEAN -> "Yes / No"
    FieldDataType.DATE -> "Date"
    FieldDataType.CHOICE -> "Choice"
    FieldDataType.MULTI_CHOICE -> "Multi-choice"
    FieldDataType.CUSTOM -> "Widget"
}

internal fun FormField.icon(): ImageVector = dataType.icon()

/** Essential standard fields can't be hidden. (Until the backend flags this, derive a sane default.) */
internal val ESSENTIAL_KEYS = setOf("name")
internal fun FormField.isEssential(): Boolean = source == FieldSource.STANDARD && fieldKey in ESSENTIAL_KEYS

/** One-line summary of a field's type + validation/options (mirrors the design's subline). */
internal fun FormField.summaryLine(): String {
    val parts = mutableListOf(dataType.typeLabel())
    validationRules?.forEach { rule ->
        when (rule.type.name) {
            "FORMAT" -> rule.kind?.let { parts.add(it.name.lowercase().replaceFirstChar { c -> c.uppercase() } + " format") }
            "LENGTH_RANGE" -> {
                val min = rule.min; val max = rule.max
                when {
                    min != null && max != null -> parts.add("$min–$max chars")
                    max != null -> parts.add("Max $max")
                    min != null -> parts.add("Min $min")
                }
            }
            "NUMBER_RANGE" -> {
                val lo = rule.minValue?.toLong(); val hi = rule.maxValue?.toLong()
                if (lo != null || hi != null) parts.add("${lo ?: ""}–${hi ?: ""}")
            }
        }
    }
    if (dataType == FieldDataType.CHOICE || dataType == FieldDataType.MULTI_CHOICE) {
        if (optionSource == OptionSource.DYNAMIC) parts.add("From ${dynamicSourceKey ?: "data"}")
        else enumValues?.let { parts.add("${it.size} option${if (it.size == 1) "" else "s"}") }
    }
    return parts.joinToString(" · ")
}
