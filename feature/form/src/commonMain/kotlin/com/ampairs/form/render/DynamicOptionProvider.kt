package com.ampairs.form.render

import kotlinx.coroutines.flow.Flow

/**
 * Supplies the selectable options for a CHOICE / MULTI_CHOICE field whose `dynamicSourceKey` matches
 * [sourceKey]. Each owning domain contributes a provider (Metro `@ContributesIntoSet(WorkspaceScope::class)`)
 * bound to a repository flow — e.g. customer types, tax codes, units. This is how the renderer
 * reproduces existing dynamic dropdowns from live, already-synced workspace data.
 *
 * NOTE (spec 011): drafted, not yet compile-gated.
 */
interface DynamicOptionProvider {
    val sourceKey: String
    fun options(): Flow<List<FieldOption>>
}

/** A selectable option: a stable [value] stored on the entity, and a human [label] for display. */
data class FieldOption(val value: String, val label: String)

/** Resolves a `dynamicSourceKey` to its provider. Injected with the Metro multibinding set. */
class DynamicOptionRegistry(providers: Set<DynamicOptionProvider>) {
    private val byKey: Map<String, DynamicOptionProvider> = providers.associateBy { it.sourceKey }
    fun forKey(sourceKey: String?): DynamicOptionProvider? = sourceKey?.let { byKey[it] }
}
