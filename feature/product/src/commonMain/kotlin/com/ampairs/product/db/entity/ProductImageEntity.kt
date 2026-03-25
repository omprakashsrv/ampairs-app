package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productImageEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "product_image_idx")
    ]
)
data class ProductImageEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val product_id: String,
    val image_id: String,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)