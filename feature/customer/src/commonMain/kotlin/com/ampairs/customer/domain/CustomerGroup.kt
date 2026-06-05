package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerGroup(
    val uid: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("group_code")
    val groupCode: String? = null,
    @SerialName("display_order")
    val displayOrder: Int? = null,
    @SerialName("default_discount_percentage")
    val defaultDiscountPercentage: Double? = null,
    @SerialName("priority_level")
    val priorityLevel: Int? = null,
    val metadata: String? = null,
    val active: Boolean = true,
    @SerialName("ref_id")
    val refId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)