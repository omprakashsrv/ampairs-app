package com.ampairs.customer.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.ampairs.customer.domain.CustomerGroup

@Entity(
    tableName = "customer_groups",
    indices = [Index(value = ["ref_id"], name = "customer_group_ref_idx")]
)
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
    val updatedAt: String?,
    val ref_id: String? = null
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
    refId = ref_id,
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
    ref_id = refId,
    createdAt = createdAt,
    updatedAt = updatedAt
)