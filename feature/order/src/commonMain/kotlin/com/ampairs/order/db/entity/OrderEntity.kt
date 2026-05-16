package com.ampairs.order.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orderEntity",
    indices = [Index(value = ["id"], unique = true, name = "order_id_idx")]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val order_number: String,
    val order_date: String,
    val status: String,
    val from_customer_id: String,
    val from_customer_name: String,
    val to_customer_name: String,
    val from_customer_gst: String,
    val to_customer_gst: String,
    val to_customer_id: String,
    val total_cost: Double,
    val total_tax: Double,
    val total_items: Long,
    val total_quantity: Double,
    val base_price: Double,
    val tax_info: String? = null,
    val created_by: String = "",
    val updated_by: String = "",
    val billing_address: String? = null,
    val shipping_address: String? = null,
    val discount: String? = null,
    val active: Long = 1,
    val soft_deleted: Long = 0,
    val synced: Long = 0,
    val last_updated: Long = 0,
    val invoice_ref_id: String? = null
)