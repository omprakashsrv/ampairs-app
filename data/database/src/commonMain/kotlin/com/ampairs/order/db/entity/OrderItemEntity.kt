package com.ampairs.order.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "orderItemEntity",
    indices = [Index(value = ["id"], unique = true, name = "order_item_id_idx")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val description: String,
    val product_id: String,
    val total_cost: Double,
    val base_price: Double,
    val product_price: Double,
    val total_tax: Double,
    val order_id: String,
    val tax_code: String,
    val quantity: Double,
    val item_no: Int,
    val selling_price: Double,
    val mrp: Double,
    val dp: Double,
    val tax_info: String? = null,
    val discount: String? = null,
    val active: Int = 1,
    val soft_deleted: Int = 0,
    // spec 010: unit of measure + base-unit quantity (FR-014) and selected variant
    val unit_id: String = "",
    val base_quantity: Double = 0.0,
    val variant_sku: String? = null,
    // spec 009: client-resolved pricing snapshot, pushed verbatim on /sync
    val resolved_unit_price_minor: Long? = null,
    val currency: String? = null,
    val price_source: String? = null,
    val matched_price_list_uid: String? = null,
    val applied_tier_min_qty: Double? = null,
    val below_moq: Int = 0,
)