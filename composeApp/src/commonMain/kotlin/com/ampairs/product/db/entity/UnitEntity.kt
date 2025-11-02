package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unitEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "unit_idx")
    ]
)
data class UnitEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String,
    val short_name: String,
    val decimal_places: Int,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)