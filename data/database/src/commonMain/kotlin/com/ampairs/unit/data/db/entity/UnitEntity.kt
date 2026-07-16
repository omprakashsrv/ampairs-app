package com.ampairs.unit.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for unit of measurement storage
 *
 * Table: units
 * Indices: id (unique), name (for search performance)
 */
@Entity(
    tableName = "units",
    indices = [
        Index(value = ["id"], unique = true, name = "unit_id_idx"),
        Index(value = ["name"], name = "unit_name_idx"),
        Index(value = ["ref_id"], name = "unit_ref_idx")
    ]
)
data class UnitEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "short_name")
    val shortName: String,

    @ColumnInfo(name = "decimal_places")
    val decimalPlaces: Int,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "active")
    val active: Boolean = true,

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,

    @ColumnInfo(name = "ref_id")
    val refId: String? = null
)
