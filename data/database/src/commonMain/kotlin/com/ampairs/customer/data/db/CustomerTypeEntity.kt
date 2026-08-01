package com.ampairs.customer.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
