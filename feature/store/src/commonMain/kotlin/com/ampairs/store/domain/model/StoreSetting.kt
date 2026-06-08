package com.ampairs.store.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A workspace-scoped setting override. Wire model for the `/setting/v1/settings` bulk sync endpoints
 * and the in-app domain model. Values are stored as strings and interpreted via [valueType].
 */
@Serializable
data class StoreSetting(
    val uid: String = "",
    val module: String = "",
    val key: String = "",
    val value: String? = null,
    @SerialName("value_type") val valueType: String = "STRING",
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
