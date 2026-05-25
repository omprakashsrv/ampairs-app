package com.ampairs.unit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Unit(
    val uid: String = "",
    val name: String = "",
    @SerialName("short_name") val shortName: String = "",
    @SerialName("decimal_places") val decimalPlaces: Int = 2,
    val description: String? = null,
    val category: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

data class UnitListItem(
    val id: String,
    val name: String,
    val shortName: String,
    val decimalPlaces: Int,
    val description: String?,
    val category: String?,
    val active: Boolean,
)
