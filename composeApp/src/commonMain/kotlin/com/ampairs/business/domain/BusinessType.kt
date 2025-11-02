package com.ampairs.business.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents supported business types.
 * Mirrors backend BusinessType enum for consistent serialization.
 */
@Serializable
enum class BusinessType {
    @SerialName("RETAIL")
    RETAIL,

    @SerialName("WHOLESALE")
    WHOLESALE,

    @SerialName("MANUFACTURING")
    MANUFACTURING,

    @SerialName("SERVICE")
    SERVICE,

    @SerialName("RESTAURANT")
    RESTAURANT,

    @SerialName("ECOMMERCE")
    ECOMMERCE,

    @SerialName("HEALTHCARE")
    HEALTHCARE,

    @SerialName("EDUCATION")
    EDUCATION,

    @SerialName("REAL_ESTATE")
    REAL_ESTATE,

    @SerialName("LOGISTICS")
    LOGISTICS,

    @SerialName("OTHER")
    OTHER
}
