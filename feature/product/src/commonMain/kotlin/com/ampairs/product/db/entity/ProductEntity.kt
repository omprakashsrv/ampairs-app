package com.ampairs.product.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "productEntity",
    indices = [
        Index(value = ["id"], unique = true, name = "product_id_idx"),
        Index(value = ["name"], name = "name_idx"),
        Index(value = ["ref_id"], name = "product_ref_idx"),
        Index(value = ["brand_id"], name = "product_brand_idx"),
        Index(value = ["category_id"], name = "product_category_idx"),
        Index(value = ["sub_category_id"], name = "product_sub_category_idx"),
        Index(value = ["group_id"], name = "product_group_idx")
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
    val description: String? = null,
    val stock_quantity: Double? = null,
    val low_stock_alert: Double? = null,
    val product_type: String? = null,
    val service_type: String? = null,
    val has_variants: Int = 0,
    val ref_id: String? = null,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    val synced: Int = 0,
    val attributes_json: String? = null
)