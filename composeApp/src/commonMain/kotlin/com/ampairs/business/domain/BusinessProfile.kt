package com.ampairs.business.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Business profile containing company information and registration details.
 */
@Serializable
data class BusinessProfile(
    @SerialName("uid")
    val uid: String = "",
    @SerialName("seq_id")
    val seqId: String = "",
    val name: String = "",
    @SerialName("business_type")
    val businessType: String = "",
    val description: String? = null,
    @SerialName("owner_name")
    val ownerName: String? = null,
    @SerialName("address_line1")
    val addressLine1: String? = null,
    @SerialName("address_line2")
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    @SerialName("tax_id")
    val taxId: String? = null,
    @SerialName("registration_number")
    val registrationNumber: String? = null,
    val active: Boolean = true,
    @SerialName("custom_attributes")
    val customAttributes: Map<String, String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Request payload for updating business profile.
 */
@Serializable
data class BusinessProfileUpdateRequest(
    val name: String,
    @SerialName("business_type")
    val businessType: String,
    val description: String? = null,
    @SerialName("owner_name")
    val ownerName: String? = null,
    @SerialName("address_line1")
    val addressLine1: String? = null,
    @SerialName("address_line2")
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    @SerialName("tax_id")
    val taxId: String? = null,
    @SerialName("registration_number")
    val registrationNumber: String? = null,
    val active: Boolean = true,
    @SerialName("custom_attributes")
    val customAttributes: Map<String, String>? = null
)
