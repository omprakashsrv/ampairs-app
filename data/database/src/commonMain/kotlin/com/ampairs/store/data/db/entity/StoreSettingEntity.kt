package com.ampairs.store.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for a workspace setting override.
 *
 * Table: store_settings. Primary key is the uid; (module, setting_key) is unique so the same logical
 * setting maps to one local row.
 */
@Entity(
    tableName = "store_settings",
    indices = [
        Index(value = ["id"], unique = true, name = "store_setting_id_idx"),
        Index(value = ["module", "setting_key"], unique = true, name = "store_setting_module_key_idx"),
    ]
)
data class StoreSettingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "module")
    val module: String,

    @ColumnInfo(name = "setting_key")
    val settingKey: String,

    @ColumnInfo(name = "value")
    val value: String? = null,

    @ColumnInfo(name = "value_type")
    val valueType: String = "STRING",

    @ColumnInfo(name = "active")
    val active: Boolean = true,

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,

    @ColumnInfo(name = "ref_id")
    val refId: String? = null,
)
