package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categoryEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "category_idx"),
        Index(value = ["ref_id"], name = "category_ref_idx")
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String,
    val image_id: String? = null,
    val ref_id: String? = null,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)