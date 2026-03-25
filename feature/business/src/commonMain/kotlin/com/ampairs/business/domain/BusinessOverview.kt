package com.ampairs.business.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Business overview for dashboard display.
 * Contains summary information about the business.
 */
@Serializable
data class BusinessOverview(
    @SerialName("uid")
    val uid: String = "",
    @SerialName("seq_id")
    val seqId: String = "",
    val name: String = "",
    @SerialName("business_type")
    val businessType: String = "",
    val currency: String = "",
    val timezone: String = "",
    val email: String? = null,
    val phone: String? = null,
    val address: String = "",
    val active: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
