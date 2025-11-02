package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageApiModel(
    @SerialName("id") val id: String = "",
    @SerialName("ref_id") val refId: String? = "",
    @SerialName("name") val name: String = "",
    @SerialName("bucket") val bucket: String = "",
    @SerialName("object_key") val objectKey: String = "",
)