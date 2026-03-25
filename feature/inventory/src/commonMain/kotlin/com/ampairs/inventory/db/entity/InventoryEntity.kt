package com.ampairs.inventory.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventoryEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "inventory_id_idx")
    ]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val description: String = "",
    val product_id: String? = null,
    val unit_id: String? = null,
    val custom_fields: String? = null,
    val mrp: Double,
    val buying_price: Double,
    val dp: Double,
    val selling_price: Double,
    val stock: Double,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0,
    val last_updated: Long? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)