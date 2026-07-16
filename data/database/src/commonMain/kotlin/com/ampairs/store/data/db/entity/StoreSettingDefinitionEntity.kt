package com.ampairs.store.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
