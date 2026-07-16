package com.ampairs.product.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "brandEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "brand_idx")
    ]
)
data class BrandEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)