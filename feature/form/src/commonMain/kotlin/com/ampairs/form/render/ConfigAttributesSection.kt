package com.ampairs.form.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FieldSource
import com.ampairs.form.domain.FormSchema

/**
 * Config-driven custom-attributes section, shared by every domain entry form (spec 011, US5).
 * Renders the schema's visible CUSTOM fields via [FormFieldRenderer] and bridges values to the
 * host's `Map<String, String>` attributes contract, so each domain's save path stays unchanged.
 * Multi-choice values are stored comma-joined. CUSTOM-dataType fields render only when a widget is
 * registered for their `widgetKey`; unbacked customs are skipped (they stay with dedicated screens).
 *
 * Renders nothing (and emits nothing) when the schema has no matching fields.
 */
@Composable
fun ConfigAttributesSection(
    schema: FormSchema?,
    optionRegistry: DynamicOptionRegistry,
    widgetRegistry: CustomFieldWidgetRegistry,
    attributes: Map<String, String>,
    onAttributesChange: (Map<String, String>) -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    val subSchema = remember(schema, widgetRegistry) {
        schema?.let { s ->
            val visibleSections = s.sections.filter { it.visible }.map { it.uid }.toSet()
            s.copy(fields = s.fields.filter {
                it.source == FieldSource.CUSTOM && it.visible && it.sectionUid in visibleSections &&
                    (it.dataType != FieldDataType.CUSTOM || it.widgetKey in widgetRegistry.keys)
            })
        }?.takeIf { it.fields.isNotEmpty() }
    } ?: return

    val initialValues = remember(subSchema) {
        subSchema.fields.associate { f ->
            val raw = attributes[f.fieldKey]
            f.fieldKey to if (f.dataType == FieldDataType.MULTI_CHOICE) {
                raw?.split(',')?.map(String::trim)?.filter { it.isNotEmpty() } ?: emptyList<String>()
            } else raw
        }
    }
    val state = rememberFormValueState(subSchema, initialValues)
    val currentAttributes by rememberUpdatedState(attributes)
    val currentOnChange by rememberUpdatedState(onAttributesChange)

    // Bridge renderer state -> the host's attributes map (managed keys only; others untouched).
    LaunchedEffect(state) {
        snapshotFlow { state.values() }.collect { values ->
            val managedKeys = subSchema.fields.map { it.fieldKey }.toSet()
            val mapped = buildMap {
                values.forEach { (k, v) ->
                    when (v) {
                        is List<*> -> v.filterIsInstance<String>().filter { it.isNotBlank() }
                            .takeIf { it.isNotEmpty() }?.let { put(k, it.joinToString(",")) }
                        is String -> if (v.isNotBlank()) put(k, v)
                        else -> Unit
                    }
                }
            }
            val unmanaged = currentAttributes.filterKeys { it !in managedKeys }
            val next = unmanaged + mapped
            if (next != currentAttributes) currentOnChange(next)
        }
    }

    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
        }
        subSchema.fields.sortedBy { it.displayOrder }.forEach { field ->
            FormFieldRenderer(
                field = field,
                state = state,
                optionRegistry = optionRegistry,
                widgetRegistry = widgetRegistry,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
