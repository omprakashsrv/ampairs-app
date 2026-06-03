package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cart with item list, subtotal and savings. See contract §4.5.
 */
@Serializable
data class CartResponse(
    @SerialName("uid") val uid: String = "",
    @SerialName("session_token") val sessionToken: String = "",
    @SerialName("status") val status: String = CartStatus.UNKNOWN.name,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("items") val items: List<CartItemResponse> = emptyList(),
    @SerialName("subtotal") val subtotal: Double = 0.0,
    @SerialName("item_total_mrp") val itemTotalMrp: Double? = null,
    @SerialName("savings") val savings: Double? = null,
)

@Serializable
data class CartItemResponse(
    @SerialName("uid") val uid: String = "",
    @SerialName("listed_product_id") val listedProductId: String = "",
    @SerialName("management_product_id") val managementProductId: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("brand") val brand: String? = null,
    @SerialName("unit") val unit: String? = null,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("mrp_at_add") val mrpAtAdd: Double? = null,
    @SerialName("quantity") val quantity: Int = 0,
    @SerialName("primary_image_url") val primaryImageUrl: String? = null,
    @SerialName("line_total") val lineTotal: Double = 0.0,
    @SerialName("line_mrp") val lineMrp: Double? = null,
)

/**
 * Add-or-update request for `POST /store/{slug}/cart/{sessionToken}/items`.
 * `quantity = 0` removes the item. See contract §4.
 */
@Serializable
data class AddCartItemRequest(
    @SerialName("listed_product_id") val listedProductId: String,
    @SerialName("quantity") val quantity: Int,
)
