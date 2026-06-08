package com.ampairs.store.domain.definition

import com.ampairs.store.domain.SettingValueType

/**
 * Client-side declaration of a setting: its key, type, default and label. Definitions live in code
 * so the generic settings UI can render them and consumers have a default before any server value
 * has synced. The effective value is `default ⊕ stored override`.
 */
data class StoreSettingDefinition(
    val module: String,
    val key: String,
    val valueType: SettingValueType,
    val defaultValue: String,
    val allowedValues: List<String> = emptyList(),
    val label: String,
    val description: String? = null,
)
