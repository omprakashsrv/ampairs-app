package com.ampairs.store.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.store.domain.SettingValueType
import com.ampairs.store.domain.definition.StoreSettingDefinition

/**
 * Room cache of the server-declared setting definitions for this workspace's installed modules.
 * Server-authoritative, pull-only (refreshed wholesale on the settings screen) — no sync metadata.
 *
 * Allowed values are stored newline-joined (values are simple enum tokens; newline avoids the
 * comma-in-value problem).
 */
@Entity(tableName = "store_setting_definitions")
data class StoreSettingDefinitionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // "$module/$key"

    @ColumnInfo(name = "module")
    val module: String,

    @ColumnInfo(name = "setting_key")
    val settingKey: String,

    @ColumnInfo(name = "value_type")
    val valueType: String,

    @ColumnInfo(name = "default_value")
    val defaultValue: String,

    @ColumnInfo(name = "allowed_values")
    val allowedValues: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "description")
    val description: String? = null,
)

fun StoreSettingDefinitionEntity.toDefinition(): StoreSettingDefinition = StoreSettingDefinition(
    module = module,
    key = settingKey,
    valueType = SettingValueType.fromName(valueType),
    defaultValue = defaultValue,
    allowedValues = if (allowedValues.isBlank()) emptyList() else allowedValues.split("\n"),
    label = label,
    description = description,
)

fun StoreSettingDefinition.toEntity(): StoreSettingDefinitionEntity = StoreSettingDefinitionEntity(
    id = "$module/$key",
    module = module,
    settingKey = key,
    valueType = valueType.name,
    defaultValue = defaultValue,
    allowedValues = allowedValues.joinToString("\n"),
    label = label,
    description = description,
)
