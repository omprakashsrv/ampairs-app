package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A counter-order pointer captured during a visit; references the `order` module order. */
@Serializable
data class FieldOrder(
    val uid: String = "",
    @SerialName("visit_uid") val visitUid: String? = null,
    @SerialName("customer_uid") val customerUid: String = "",
    @SerialName("rep_member_uid") val repMemberUid: String = "",
    @SerialName("order_uid") val orderUid: String? = null,
    val amount: Double = 0.0,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
