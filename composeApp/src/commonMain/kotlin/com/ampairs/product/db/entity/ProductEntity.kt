package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "product_id_idx"),
        Index(value = ["name"], name = "name_idx")
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String = "",
    val code: String = "",
    val group_id: String? = null,
    val brand_id: String? = null,
    val category_id: String? = null,
    val sub_category_id: String? = null,
    val tax_code: String,
    val base_unit: String? = null,
    val last_updated: Long? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val mrp: Double,
    val dp: Double,
    val selling_price: Double,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0
)