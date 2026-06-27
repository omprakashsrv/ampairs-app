package com.ampairs.supplier.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Supplier(
    val uid: String = "",
    @SerialName("ref_id")
    val refId: String? = null,
    val name: String = "",
    val email: String? = null,
    val phone: String? = null,
    val landline: String? = null,
    @SerialName("country_code")
    val countryCode: Int = 91,
    @SerialName("supplier_type")
    val supplierType: String? = null,
    @SerialName("supplier_group")
    val supplierGroup: String? = null,
    @SerialName("gst_number")
    val gstNumber: String? = null,
    @SerialName("pan_number")
    val panNumber: String? = null,
    @SerialName("credit_limit")
    val creditLimit: Double? = null,
    @SerialName("credit_days")
    val creditDays: Int? = null,
    @SerialName("outstanding_amount")
    val outstandingAmount: Double? = null,
    val address: String? = null,
    val street: String? = null,
    val street2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val country: String = "India",
    @SerialName("billing_address")
    val billingAddress: SupplierAddress? = null,
    @SerialName("shipping_address")
    val shippingAddress: SupplierAddress? = null,
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
data class SupplierAddress(
    val street: String? = null,
    @SerialName("street2")
    val street2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val country: String? = null
)

data class SupplierListItem(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val city: String?,
    val gstin: String? = null,
    val state: String? = null,
)

fun Supplier.toListItem(): SupplierListItem = SupplierListItem(
    id = uid,
    name = name,
    phone = phone,
    email = email,
    city = city,
    gstin = gstNumber,
    state = state,
)
