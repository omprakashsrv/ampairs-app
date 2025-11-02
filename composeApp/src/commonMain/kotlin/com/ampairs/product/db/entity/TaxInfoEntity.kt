package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "taxInfoEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "tax_info_id_idx")
    ]
)
data class TaxInfoEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String,
    val formatted_name: String,
    val tax_spec: String,
    val percentage: Double,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)