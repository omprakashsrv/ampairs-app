package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unitConversionEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "unit_conversion_idx")
    ]
)
data class UnitConversionEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val product_id: String,
    val base_unit_id: String,
    val derived_unit_id: String,
    val multiplier: Double,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)