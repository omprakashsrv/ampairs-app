package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UnitApiModel(
    @SerialName("id") var id: String = "",
    @SerialName("ref_id") var refId: String? = null,
    @SerialName("name") var name: String = "",
    @SerialName("short_name") var shortName: String = "",
    @SerialName("decimal_places") var decimalPlaces: Int = 0,
    @SerialName("active") val active: Boolean = true,
    @SerialName("soft_deleted") val softDeleted: Boolean = false,
)