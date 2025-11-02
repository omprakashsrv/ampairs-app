package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductCategoryApiModel(
    @SerialName("products") val products: List<ProductApiModel>,
    @SerialName("categories") val categories: List<ProductGroupApiModel>,
)