package com.ampairs.unit.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.unit.domain.model.Unit

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

/**
 * Convert entity to domain model
 */
fun UnitEntity.toUnit(): Unit = Unit(
    uid = id,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    description = description,
    category = category,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Convert domain model to entity
 */
fun Unit.toEntity(): UnitEntity = UnitEntity(
    id = uid,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    description = description,
    category = category,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
