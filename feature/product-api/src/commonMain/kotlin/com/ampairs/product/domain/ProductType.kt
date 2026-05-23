package com.ampairs.product.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProductType {
    @SerialName("RETAIL") RETAIL,
    @SerialName("WHOLESALE") WHOLESALE,
    @SerialName("SERVICE") SERVICE;

    val displayName: String
        get() = when (this) {
            RETAIL -> "Retail"
            WHOLESALE -> "Wholesale"
            SERVICE -> "Service"
        }
}

@Serializable
enum class ServiceType {
    @SerialName("PHYSICAL") PHYSICAL,
    @SerialName("SERVICE") SERVICE,
    @SerialName("DIGITAL") DIGITAL;

    val displayName: String
        get() = when (this) {
            PHYSICAL -> "Physical Product"
            SERVICE -> "Service"
            DIGITAL -> "Digital Product"
        }
}
