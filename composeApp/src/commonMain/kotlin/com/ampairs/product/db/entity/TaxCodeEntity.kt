package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "taxCodeEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "tax_code_id_idx")
    ]
)
data class TaxCodeEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val code: String = "",
    val type: String,
    val description: String = "",
    val effective_from: String? = null,
    val tax_info: String,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)