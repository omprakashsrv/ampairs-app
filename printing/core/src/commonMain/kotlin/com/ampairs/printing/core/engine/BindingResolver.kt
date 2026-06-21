package com.ampairs.printing.core.engine

import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldBinding
import com.ampairs.printing.core.model.FieldSource
import com.ampairs.printing.core.model.FieldValue

/**
 * Pure (non-suspend) binding resolution — the heart of §7 field mapping, kept side-effect-free so it
 * is directly unit-testable. Resolution = two map lookups (standard/custom) or a precomputed value,
 * with fallback. Mirrors feature/form's standard-map + attributes-map bridge.
 */
object BindingResolver {

    fun resolve(
        binding: FieldBinding,
        standard: Map<String, FieldValue>,
        custom: Map<String, String>,
        nested: Map<EntityRef, Map<String, FieldValue>> = emptyMap(),
        computed: FieldValue? = null,
    ): FieldValue {
        val raw: FieldValue? = when (binding.source) {
            FieldSource.STANDARD ->
                if (binding.entityRef == EntityRef.SELF) standard[binding.fieldKey]
                else nested[binding.entityRef]?.get(binding.fieldKey)

            FieldSource.CUSTOM ->
                custom[binding.fieldKey]?.let { FieldValue.Text(it) }

            FieldSource.COMPUTED -> computed
        }

        return raw?.takeUnless { it.isBlank() }
            ?: binding.fallback?.let { FieldValue.Text(it) }
            ?: FieldValue.Empty
    }

    private fun FieldValue.isBlank(): Boolean =
        this is FieldValue.Empty || (this is FieldValue.Text && value.isBlank())
}
