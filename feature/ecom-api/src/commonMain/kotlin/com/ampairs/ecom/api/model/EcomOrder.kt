package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Delivery address embedded in an order. See contract §7.3.
 */
@Serializable
data class DeliveryAddress(
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String? = null,
    @SerialName("city") val city: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("pin_code") val pinCode: String = "",
    @SerialName("country") val country: String = "IN",
    @SerialName("phone") val phone: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
)

@Serializable
data class EcomOrderLineItem(
    @SerialName("uid") val uid: String = "",
    @SerialName("listed_product_id") val listedProductId: String = "",
    @SerialName("management_product_id") val managementProductId: String? = null,
    @SerialName("product_name") val productName: String = "",
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("quantity_ordered") val quantityOrdered: Int = 0,
    @SerialName("quantity_confirmed") val quantityConfirmed: Int? = null,
    @SerialName("line_total") val lineTotal: Double = 0.0,
    @SerialName("status") val status: String = LineItemStatus.UNKNOWN.name,
)

/**
 * Order detail + live status. See contract §7.3.
 */
@Serializable
data class EcomOrderResponse(
    @SerialName("uid") val uid: String = "",
    @SerialName("ecom_order_ref") val ecomOrderRef: String = "",
    @SerialName("storefront_id") val storefrontId: String = "",
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("delivery_address") val deliveryAddress: DeliveryAddress? = null,
    @SerialName("status") val status: String = OrderStatus.UNKNOWN.name,
    @SerialName("management_order_ref") val managementOrderRef: String? = null,
    @SerialName("line_items") val lineItems: List<EcomOrderLineItem> = emptyList(),
    @SerialName("subtotal") val subtotal: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("notes") val notes: String? = null,
    @SerialName("placed_at") val placedAt: String = "",
    @SerialName("confirmed_at") val confirmedAt: String? = null,
)

/**
 * Checkout payload for `POST /store/{slug}/cart/{sessionToken}/checkout`. See contract §5.
 * Provide either [deliveryAddressId] (saved) OR [deliveryAddress] (inline) — not both.
 */
@Serializable
data class CheckoutRequest(
    @SerialName("delivery_address_id") val deliveryAddressId: String? = null,
    @SerialName("delivery_address") val deliveryAddress: AddressRequest? = null,
    @SerialName("save_address") val saveAddress: Boolean = false,
    @SerialName("notes") val notes: String? = null,
)
