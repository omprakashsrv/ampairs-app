package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerType(
    val uid: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("type_code")
    val typeCode: String? = null,
    @SerialName("display_order")
    val displayOrder: Int? = null,
    @SerialName("default_credit_limit")
    val defaultCreditLimit: Double? = null,
    @SerialName("default_credit_days")
    val defaultCreditDays: Int? = null,
    val metadata: String? = null,
    val active: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)