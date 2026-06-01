package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ProductGroupApiModel(
    @SerialName("id") val id: String?,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("active") val active: Boolean = true,
    @SerialName("soft_deleted") val softDeleted: Boolean = false,
    @SerialName("image_id") val imageId: String? = null,
    @SerialName("image") val image: ImageApiModel? = null,
)