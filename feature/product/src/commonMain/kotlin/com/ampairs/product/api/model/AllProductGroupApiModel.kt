package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class AllProductGroupApiModel(
    @SerialName("groups") val groups: List<ProductGroupApiModel>?,
    @SerialName("categories") val categories: List<ProductGroupApiModel>?,
    @SerialName("brands") val brands: List<ProductGroupApiModel>?,
    @SerialName("sub_categories") val subCategories: List<ProductGroupApiModel>?,
)