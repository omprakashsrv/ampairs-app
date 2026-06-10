package com.ampairs.form.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.render.CustomFieldWidget
import com.ampairs.form.render.CustomFieldWidgetRegistry
import com.ampairs.form.render.DynamicOptionProvider
import com.ampairs.form.render.DynamicOptionRegistry
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides

/**
 * Wires the schema-driven renderer's registries (spec 011, US1). Domains contribute their
 * [DynamicOptionProvider]s (dynamic dropdowns) and [CustomFieldWidget]s (escape-hatch inputs) via
 * Metro `@ContributesIntoSet(WorkspaceScope::class)`; here we collect the (possibly empty) sets into
 * the registries the renderer injects.
 *
 * NOTE (spec 011): first use of Metro multibindings in this codebase — validate `@Multibinds` /
 * `allowEmpty` against the compiler; adjust the annotation if Metro's API differs.
 */
@ContributesTo(WorkspaceScope::class)
interface FormRenderModule {

    @Multibinds(allowEmpty = true)
    fun dynamicOptionProviders(): Set<DynamicOptionProvider>

    @Multibinds(allowEmpty = true)
    fun customFieldWidgets(): Set<CustomFieldWidget>

    companion object {
        @Provides
        fun provideDynamicOptionRegistry(providers: Set<DynamicOptionProvider>): DynamicOptionRegistry =
            DynamicOptionRegistry(providers)

        @Provides
        fun provideCustomFieldWidgetRegistry(widgets: Set<CustomFieldWidget>): CustomFieldWidgetRegistry =
            CustomFieldWidgetRegistry(widgets)
    }
}
