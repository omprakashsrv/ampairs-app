package com.ampairs.customer.ui.create

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
import com.ampairs.form.render.CustomFieldWidgetRegistry
import com.ampairs.form.render.DynamicOptionRegistry
import com.ampairs.form.render.FormFieldRenderer
import com.ampairs.form.render.rememberFormValueState

/**
 * Config-driven STANDARD fields of the customer form (spec 011, US1). Renders the unified schema's
 * visible standard fields — sectioned and ordered per workspace configuration — through
 * [FormFieldRenderer], bridging values to [CustomerFormState] via [toStandardValueMap] /
 * [applyStandardValues] so the save path and VM validation stay unchanged.
 *
 * [reseedKey] re-seeds the renderer state when the backing form state is overwritten externally
 * (contact import). Widget-kind fields and non-registry blocks (billing/shipping, location, images)
 * remain domain-rendered by the host screen.
 */
@Composable
fun CustomerStandardFields(
    schema: FormSchema?,
    optionRegistry: DynamicOptionRegistry,
    widgetRegistry: CustomFieldWidgetRegistry,
    formState: CustomerFormState,
    onFormChange: (CustomerFormState) -> Unit,
    reseedKey: Any?,
    modifier: Modifier = Modifier,
) {
    val subSchema = remember(schema) {
        schema?.let { s ->
            val visibleSections = s.sections.filter { it.visible }.map { it.uid }.toSet()
            s.copy(fields = s.fields.filter {
                it.source == FieldSource.STANDARD && it.visible &&
                    it.dataType != FieldDataType.CUSTOM && it.sectionUid in visibleSections
            })
        }?.takeIf { it.fields.isNotEmpty() }
    }
    if (subSchema == null) {
        Text(
            "Form configuration is syncing…",
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val initialValues = remember(subSchema, reseedKey) { formState.toStandardValueMap() }
    val state = rememberFormValueState(subSchema, initialValues, reseedKey)
    val currentForm by rememberUpdatedState(formState)
    val currentOnChange by rememberUpdatedState(onFormChange)

    LaunchedEffect(state) {
        snapshotFlow { state.values() }.collect { values ->
            val next = currentForm.applyStandardValues(values)
            if (next != currentForm) currentOnChange(next)
        }
    }

    Column(modifier.fillMaxWidth()) {
        subSchema.sectionsWithFields().forEach { (section, fields) ->
            Text(
                section.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            fields.forEach { field ->
                FormFieldRenderer(
                    field = field,
                    state = state,
                    optionRegistry = optionRegistry,
                    widgetRegistry = widgetRegistry,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
