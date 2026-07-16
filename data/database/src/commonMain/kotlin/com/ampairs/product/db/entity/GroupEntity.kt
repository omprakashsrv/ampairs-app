package com.ampairs.product.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "groupEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "group_idx"),
        Index(value = ["ref_id"], name = "group_ref_idx")
    ]
)
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String,
    val ref_id: String? = null,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)