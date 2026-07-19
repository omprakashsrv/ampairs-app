package com.ampairs.ecom.data.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Saved delivery address (offline-first CRUD). Contract §8 `customer_address`. */
@Entity(tableName = "customer_address")
data class CustomerAddressEntity(
    @PrimaryKey val uid: String,
    val label: String? = null,
    val address_line1: String,
    val address_line2: String? = null,
    val city: String,
    val state: String,
    val pin_code: String,
    val country: String = "IN",
    val phone: String? = null,
    val is_default: Int = 0,
    val active: Int = 1,
    val synced: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/** Cached order header. Contract §8 `ecom_order`. `delivery_address` is a JSON blob. */
@Entity(
    tableName = "ecom_order",
    indices = [Index(value = ["ecom_order_ref"], unique = true, name = "ecom_order_ref_idx")]
)
data class EcomOrderEntity(
    @PrimaryKey val uid: String,
    val ecom_order_ref: String,
    /** Human-friendly order number (e.g. "ECO-00001") — what the buyer sees/quotes to track this order. */
    val order_number: String = "",
    val storefront_id: String,
    val status: String,
    val subtotal: Double,
    val total_amount: Double,
    val notes: String? = null,
    val placed_at: String,
    val confirmed_at: String? = null,
    val delivery_address: String? = null,
)

/** Cached order line item. Contract §8 `ecom_order_line_item`. */
@Entity(
    tableName = "ecom_order_line_item",
    indices = [Index(value = ["order_uid"], name = "ecom_line_order_idx")]
)
data class EcomOrderLineItemEntity(
    @PrimaryKey val uid: String,
    val order_uid: String,
    val listed_product_id: String,
    val product_name: String,
    val unit_price: Double,
    val quantity_ordered: Int,
    val quantity_confirmed: Int? = null,
    val line_total: Double,
    val status: String,
)
