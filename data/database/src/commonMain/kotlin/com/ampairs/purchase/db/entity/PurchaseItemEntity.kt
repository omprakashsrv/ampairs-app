package com.ampairs.purchase.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "purchaseItemEntity",
    indices = [Index(value = ["id"], unique = true, name = "purchase_item_id_idx")]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val description: String,
    val product_id: String,
    val total_cost: Double,
    val base_price: Double,
    val product_price: Double,
    val total_tax: Double,
    val purchase_id: String,
    val tax_code: String,
    val quantity: Double,
    val item_no: Int,
    // buy-side unit price (the cost paid per unit to the supplier)
    val price: Double,
    val mrp: Double,
    val dp: Double,
    val tax_info: String? = null,
    val discount: String? = null,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    // unit of measure + base-unit quantity (mirrors order FR-014) and selected variant
    val unit_id: String = "",
    val base_quantity: Double = 0.0,
    val variant_sku: String? = null
)
