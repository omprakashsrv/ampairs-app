package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Note: CustomerType enum removed - now using dynamic CustomerType entity
// Customer types are now managed as master data through CustomerTypeRepository

@Serializable
data class Customer(
    val uid: String = "",
    @SerialName("ref_id")
    val refId: String? = null,
    val name: String = "",
    val email: String? = null,
    val phone: String? = null,
    val landline: String? = null,
    @SerialName("country_code")
    val countryCode: Int = 91,
    @SerialName("customer_type")
    val customerType: String? = null, // Stores customer type name, not UID
    @SerialName("customer_group")
    val customerGroup: String? = null, // Stores customer group name, not UID
    @SerialName("gst_number")
    val gstNumber: String? = null,
    @SerialName("pan_number")
    val panNumber: String? = null,
    @SerialName("credit_limit")
    val creditLimit: Double? = null,
    @SerialName("credit_days")
    val creditDays: Int? = null,
    val address: String? = null,
    val street: String? = null,
    val street2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val country: String = "India",
    @SerialName("billing_address")
    val billingAddress: CustomerAddress? = null,
    @SerialName("shipping_address")
    val shippingAddress: CustomerAddress? = null,
    val status: String? = null,
    val attributes: Map<String, String>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val active: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CustomerAddress(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "India"
)

data class CustomerListItem(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val city: String?,
    val primaryThumbnailUrl: String? = null
)

fun Customer.toListItem(primaryThumbnailUrl: String? = null): CustomerListItem = CustomerListItem(
    id = uid,
    name = name,
    phone = phone,
    email = email,
    city = city,
    primaryThumbnailUrl = primaryThumbnailUrl
)