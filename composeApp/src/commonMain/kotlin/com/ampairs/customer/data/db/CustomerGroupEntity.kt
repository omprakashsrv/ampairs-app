package com.ampairs.customer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.customer.domain.CustomerGroup

@Entity(tableName = "customer_groups")
data class CustomerGroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val groupCode: String?,
    val displayOrder: Int?,
    val defaultDiscountPercentage: Double?,
    val priorityLevel: Int?,
    val metadata: String?,
    val active: Boolean,
    val synced: Boolean = false,
    val createdAt: String?,
    val updatedAt: String?
)

fun CustomerGroupEntity.toCustomerGroup(): CustomerGroup = CustomerGroup(
    uid = id,
    name = name,
    description = description,
    groupCode = groupCode,
    displayOrder = displayOrder,
    defaultDiscountPercentage = defaultDiscountPercentage,
    priorityLevel = priorityLevel,
    metadata = metadata,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomerGroup.toEntity(): CustomerGroupEntity = CustomerGroupEntity(
    id = uid,
    name = name,
    description = description,
    groupCode = groupCode,
    displayOrder = displayOrder,
    defaultDiscountPercentage = defaultDiscountPercentage,
    priorityLevel = priorityLevel,
    metadata = metadata,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)