package com.ampairs.order.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val soft_deleted: Int = 0
)