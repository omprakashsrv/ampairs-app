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
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

data class CustomerGroupListItem(
    val id: String,
    val name: String,
    val description: String?,
    val groupCode: String?,
    val displayOrder: Int?,
    val defaultDiscountPercentage: Double?,
    val priorityLevel: Int?,
    val active: Boolean
)

fun CustomerGroup.toListItem(): CustomerGroupListItem = CustomerGroupListItem(
    id = uid,
    name = name,
    description = description,
    groupCode = groupCode,
    displayOrder = displayOrder,
    defaultDiscountPercentage = defaultDiscountPercentage,
    priorityLevel = priorityLevel,
    active = active
)