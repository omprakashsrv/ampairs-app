package com.ampairs.customer.ui.create

import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_section_attributes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.render.ConfigAttributesSection
import com.ampairs.form.render.CustomFieldWidgetRegistry
import com.ampairs.form.render.DynamicOptionRegistry
import org.jetbrains.compose.resources.stringResource

/**
 * Customer wrapper over the shared [ConfigAttributesSection] (spec 011, US1/US5) — renders the
 * schema's visible CUSTOM fields and bridges values to the existing `Map<String, String>`
 * attributes contract, so the save path is unchanged.
 */
@Composable
fun CustomerCustomAttributes(
    schema: FormSchema?,
    optionRegistry: DynamicOptionRegistry,
    widgetRegistry: CustomFieldWidgetRegistry,
    attributes: Map<String, String>,
    onAttributesChange: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfigAttributesSection(
        schema = schema,
        optionRegistry = optionRegistry,
        widgetRegistry = widgetRegistry,
        attributes = attributes,
        onAttributesChange = onAttributesChange,
        title = stringResource(Res.string.customer_section_attributes),
        modifier = modifier,
    )
}
