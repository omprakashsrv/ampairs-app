package com.ampairs.form.render

import androidx.compose.runtime.Composable
import com.ampairs.form.domain.FormField

/**
 * Escape hatch for inputs the generic renderers can't express (image gallery, structured address,
 * map/location, business hours). The renderer places/orders/gates a `data_type = CUSTOM` field by its
 * config and delegates the input control to the domain-provided widget whose [widgetKey] matches the
 * field's `widgetKey`. Domains contribute via Metro `@ContributesIntoSet(WorkspaceScope::class)`.
 *
 * NOTE (spec 011): drafted, not yet compile-gated.
 */
interface CustomFieldWidget {
    val widgetKey: String

    @Composable
    fun Render(
        field: FormField,
        value: String?,
        onValueChange: (String?) -> Unit,
        enabled: Boolean,
        error: String?,
    )
}

/** Resolves a `widgetKey` to its widget. Injected with the Metro multibinding set. */
class CustomFieldWidgetRegistry(widgets: Set<CustomFieldWidget>) {
    private val byKey: Map<String, CustomFieldWidget> = widgets.associateBy { it.widgetKey }
    /** All registered widget keys (so a renderer can include only CUSTOM fields it can actually draw). */
    val keys: Set<String> get() = byKey.keys
    fun forKey(widgetKey: String?): CustomFieldWidget? = widgetKey?.let { byKey[it] }
}
