package com.ampairs.store.domain.definition

import com.ampairs.store.domain.SettingValueType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A setting the server declares for this workspace's installed modules. Fetched from
 * `GET /setting/v1/definitions`, cached in Room, and used to render the generic settings screen.
 * The default lives here so the screen can show a value before any override has synced.
 *
 * Note: programmatic consumers (invoice/order) do NOT rely on this catalog — they pass their own
 * default at the call site (`StoreSettingsProvider.getBoolean(module, key, default)`), so they work
 * offline before the catalog has been fetched.
 */
@Serializable
data class StoreSettingDefinition(
    val module: String = "",
    val key: String = "",
    @SerialName("value_type") val valueType: SettingValueType = SettingValueType.STRING,
    @SerialName("default_value") val defaultValue: String = "",
    @SerialName("allowed_values") val allowedValues: List<String> = emptyList(),
    val label: String = "",
    val description: String? = null,
)
