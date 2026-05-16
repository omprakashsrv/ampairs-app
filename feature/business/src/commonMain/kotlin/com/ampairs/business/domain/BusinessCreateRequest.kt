package com.ampairs.business.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request for creating a new business profile.
 */
@Serializable
data class BusinessCreateRequest(
    @SerialName("name")
    val name: String,

    @SerialName("business_type")
    val businessType: String,

    @SerialName("description")
    val description: String? = null,

    @SerialName("owner_name")
    val ownerName: String? = null,

    // Address
    @SerialName("address_line1")
    val addressLine1: String? = null,

    @SerialName("address_line2")
    val addressLine2: String? = null,

    @SerialName("city")
    val city: String? = null,

    @SerialName("state")
    val state: String? = null,

    @SerialName("postal_code")
    val postalCode: String? = null,

    @SerialName("country")
    val country: String? = null,

    // Location
    @SerialName("latitude")
    val latitude: Double? = null,

    @SerialName("longitude")
    val longitude: Double? = null,

    // Contact
    @SerialName("phone")
    val phone: String? = null,

    @SerialName("email")
    val email: String? = null,

    @SerialName("website")
    val website: String? = null,

    // Tax/Regulatory
    @SerialName("tax_id")
    val taxId: String? = null,

    @SerialName("registration_number")
    val registrationNumber: String? = null,

    // Operational Config
    @SerialName("timezone")
    val timezone: String = "UTC",

    @SerialName("currency")
    val currency: String = "INR",

    @SerialName("language")
    val language: String = "en",

    @SerialName("date_format")
    val dateFormat: String = "DD-MM-YYYY",

    @SerialName("time_format")
    val timeFormat: String = "12H",

    // Business Hours
    @SerialName("opening_hours")
    val openingHours: String? = null,

    @SerialName("closing_hours")
    val closingHours: String? = null,

    @SerialName("operating_days")
    val operatingDays: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
)
