package com.ampairs.form.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.form.domain.FormSchema

/**
 * Renders a complete entry form from a [FormSchema] + [FormValueState]: visible sections in order,
 * each with its visible fields, every input bound two-way to the state with inline validation. This
 * is the single runtime every domain's entry screen renders through (the "missing runtime").
 *
 * @param readOnly when true (e.g. the admin live-preview), inputs render but edits are inert at the
 *        screen level — callers typically pass a throwaway [FormValueState].
 *
 * NOTE (spec 011): drafted, not yet compile-gated. Validate across Android/iOS/Desktop before use.
 */
@Composable
fun DynamicFormRenderer(
    schema: FormSchema,
    state: FormValueState,
    optionRegistry: DynamicOptionRegistry,
    widgetRegistry: CustomFieldWidgetRegistry,
    modifier: Modifier = Modifier,
) {
    val sections = schema.sectionsWithFields()
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        sections.forEach { (section, fields) ->
            item(key = "section-${section.uid}") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = section.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    HorizontalDivider()
                }
            }
            items(fields, key = { it.uid }) { field ->
                FormFieldRenderer(
                    field = field,
                    state = state,
                    optionRegistry = optionRegistry,
                    widgetRegistry = widgetRegistry,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
