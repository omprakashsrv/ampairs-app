package com.ampairs.business.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Business operations containing operational settings.
 */
@Serializable
data class BusinessOperations(
    @SerialName("uid")
    val uid: String = "",
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
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Request payload for updating business operations.
 */
@Serializable
data class BusinessOperationsUpdateRequest(
    val timezone: String,
    val currency: String,
    val language: String,
    @SerialName("date_format")
    val dateFormat: String,
    @SerialName("time_format")
    val timeFormat: String,
    @SerialName("opening_hours")
    val openingHours: String? = null,
    @SerialName("closing_hours")
    val closingHours: String? = null,
    @SerialName("operating_days")
    val operatingDays: List<String> = emptyList()
)
