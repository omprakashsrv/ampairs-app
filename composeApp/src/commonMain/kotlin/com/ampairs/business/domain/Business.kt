package com.ampairs.business.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain representation of business profile information.
 */
@Serializable
data class Business(
    @SerialName("uid")
    val id: String = "",
    @SerialName("seq_id")
    val seqId: String? = null,
    @SerialName("workspace_id")
    val workspaceId: String? = null,
    val name: String = "",
    @SerialName("business_type")
    val businessType: BusinessType = BusinessType.RETAIL,
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
    @SerialName("tax_settings")
    val taxSettings: Map<String, String>? = null,
    val timezone: String = "UTC",
    val currency: String = "INR",
    val language: String = "en",
    @SerialName("date_format")
    val dateFormat: String = "DD-MM-YYYY",
    @SerialName("time_format")
    val timeFormat: String = "12H",
    @SerialName("opening_hours")
    val openingHours: String? = null,
    @SerialName("closing_hours")
    val closingHours: String? = null,
    @SerialName("operating_days")
    val operatingDays: List<String> = emptyList(),
    val active: Boolean = true,
    @SerialName("custom_attributes")
    val customAttributes: Map<String, String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("updated_by")
    val updatedBy: String? = null
)

/**
 * Payload used for create/update requests.
 * Excludes immutable audit fields.
 */
@Serializable
data class BusinessPayload(
    val name: String,
    @SerialName("business_type")
    val businessType: BusinessType,
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
    @SerialName("tax_settings")
    val taxSettings: Map<String, String>? = null,
    val timezone: String = "UTC",
    val currency: String = "INR",
    val language: String = "en",
    @SerialName("date_format")
    val dateFormat: String = "DD-MM-YYYY",
    @SerialName("time_format")
    val timeFormat: String = "12H",
    @SerialName("opening_hours")
    val openingHours: String? = null,
    @SerialName("closing_hours")
    val closingHours: String? = null,
    @SerialName("operating_days")
    val operatingDays: List<String> = emptyList(),
    val active: Boolean = true,
    @SerialName("custom_attributes")
    val customAttributes: Map<String, String>? = null
)

/**
 * Convert domain model to payload for create/update operations.
 */
fun Business.toPayload(): BusinessPayload = BusinessPayload(
    name = name,
    businessType = businessType,
    description = description,
    ownerName = ownerName,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    city = city,
    state = state,
    postalCode = postalCode,
    country = country,
    latitude = latitude,
    longitude = longitude,
    phone = phone,
    email = email,
    website = website,
    taxId = taxId,
    registrationNumber = registrationNumber,
    taxSettings = taxSettings,
    timezone = timezone,
    currency = currency,
    language = language,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
    openingHours = openingHours,
    closingHours = closingHours,
    operatingDays = operatingDays,
    active = active,
    customAttributes = customAttributes
)
