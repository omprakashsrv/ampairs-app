package com.ampairs.customer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.customer.domain.CustomerType

@Entity(tableName = "customer_types")
data class CustomerTypeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val typeCode: String?,
    val displayOrder: Int?,
    val defaultCreditLimit: Double?,
    val defaultCreditDays: Int?,
    val metadata: String?,
    val active: Boolean,
    val synced: Boolean = false,
    val createdAt: String?,
    val updatedAt: String?
)

fun CustomerTypeEntity.toCustomerType(): CustomerType = CustomerType(
    uid = id,
    name = name,
    description = description,
    typeCode = typeCode,
    displayOrder = displayOrder,
    defaultCreditLimit = defaultCreditLimit,
    defaultCreditDays = defaultCreditDays,
    metadata = metadata,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomerType.toEntity(): CustomerTypeEntity = CustomerTypeEntity(
    id = uid,
    name = name,
    description = description,
    typeCode = typeCode,
    displayOrder = displayOrder,
    defaultCreditLimit = defaultCreditLimit,
    defaultCreditDays = defaultCreditDays,
    metadata = metadata,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)