package com.ampairs.ecom.data.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Active cart per storefront. Contract §8 `cart`. */
@Entity(
    tableName = "cart",
    indices = [Index(value = ["session_token"], unique = true, name = "cart_session_idx")]
)
data class CartEntity(
    @PrimaryKey val uid: String,
    val storefront_id: String,
    val session_token: String,
    val status: String = "ACTIVE",
    val expires_at: String? = null,
)

/** Cart line item — optimistic local mirror of the server cart. Contract §8 `cart_item`. */
@Entity(
    tableName = "cart_item",
    indices = [Index(value = ["cart_id"], name = "cart_item_cart_idx")]
)
data class CartItemEntity(
    @PrimaryKey val uid: String,
    val cart_id: String,
    val listed_product_id: String,
    val management_product_id: String? = null,
    val product_name: String,
    val brand: String? = null,
    val unit: String? = null,
    val unit_price: Double,
    val mrp_at_add: Double? = null,
    val quantity: Int,
    val primary_image_url: String? = null,
)
