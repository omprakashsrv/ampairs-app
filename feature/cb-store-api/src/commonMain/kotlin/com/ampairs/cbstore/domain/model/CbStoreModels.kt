package com.ampairs.cbstore.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A California Burrito zonal office. Matches the backend `cb_store` ZonalOfficeResponse. */
@Serializable
data class ZonalOffice(
    val uid: String = "",
    val name: String = "",
    val city: String = "",
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** A California Burrito outlet. Matches the backend `cb_store` StoreResponse. */
@Serializable
data class Store(
    val uid: String = "",
    val code: String = "",
    val name: String = "",
    val city: String = "",
    @SerialName("zonal_office_id") val zonalOfficeId: String = "",
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
