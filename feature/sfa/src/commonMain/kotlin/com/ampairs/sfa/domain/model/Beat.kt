package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A beat / route — an ordered set of outlets a field rep covers. Wire model for `/sfa/v1/beats/sync`. */
@Serializable
data class Beat(
    val uid: String = "",
    val name: String = "",
    val description: String? = null,
    @SerialName("rep_member_uid") val repMemberUid: String? = null,
    @SerialName("scheduled_days") val scheduledDays: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
