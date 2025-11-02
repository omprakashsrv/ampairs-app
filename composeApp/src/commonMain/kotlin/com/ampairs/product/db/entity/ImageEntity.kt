package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "imageEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "image_idx")
    ]
)
data class  ImageEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String,
    val bucket: String,
    val object_key: String
)