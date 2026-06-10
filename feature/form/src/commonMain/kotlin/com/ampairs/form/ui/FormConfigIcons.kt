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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormField
import com.ampairs.form.domain.OptionSource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.form.generated.resources.Res
import ampairsapp.feature.form.generated.resources.*

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

internal fun FieldDataType.typeLabelRes(): StringResource = when (this) {
    FieldDataType.TEXT -> Res.string.form_type_text
    FieldDataType.TEXTAREA -> Res.string.form_type_paragraph
    FieldDataType.NUMBER -> Res.string.form_type_number
    FieldDataType.BOOLEAN -> Res.string.form_type_yesno
    FieldDataType.DATE -> Res.string.form_type_date
    FieldDataType.CHOICE -> Res.string.form_type_choice
    FieldDataType.MULTI_CHOICE -> Res.string.form_type_multi_choice
    FieldDataType.CUSTOM -> Res.string.form_type_widget
}

@Composable
internal fun FieldDataType.typeLabel(): String = stringResource(typeLabelRes())

internal fun FormField.icon(): ImageVector = dataType.icon()

/** Essential standard fields can't be hidden. (Until the backend flags this, derive a sane default.) */
internal val ESSENTIAL_KEYS = setOf("name")
internal fun FormField.isEssential(): Boolean = source == FieldSource.STANDARD && fieldKey in ESSENTIAL_KEYS

/** One-line summary of a field's type + validation/options (mirrors the design's subline). */
@Composable
internal fun FormField.summaryLine(): String {
    val parts = mutableListOf(dataType.typeLabel())
    validationRules?.forEach { rule ->
        when (rule.type.name) {
            "FORMAT" -> rule.kind?.let {
                parts.add(stringResource(Res.string.form_summary_format, it.name.lowercase().replaceFirstChar { c -> c.uppercase() }))
            }
            "LENGTH_RANGE" -> {
                val min = rule.min; val max = rule.max
                when {
                    min != null && max != null -> parts.add(stringResource(Res.string.form_summary_chars, min, max))
                    max != null -> parts.add(stringResource(Res.string.form_summary_max, max))
                    min != null -> parts.add(stringResource(Res.string.form_summary_min, min))
                }
            }
            "NUMBER_RANGE" -> {
                val lo = rule.minValue?.toLong(); val hi = rule.maxValue?.toLong()
                if (lo != null || hi != null) parts.add("${lo ?: ""}–${hi ?: ""}")
            }
        }
    }
    if (dataType == FieldDataType.CHOICE || dataType == FieldDataType.MULTI_CHOICE) {
        if (optionSource == OptionSource.DYNAMIC) {
            parts.add(stringResource(Res.string.form_summary_from, dynamicSourceKey ?: stringResource(Res.string.form_summary_data)))
        } else {
            enumValues?.let { parts.add(pluralStringResource(Res.plurals.form_summary_options, it.size, it.size)) }
        }
    }
    return parts.joinToString(" · ")
}
