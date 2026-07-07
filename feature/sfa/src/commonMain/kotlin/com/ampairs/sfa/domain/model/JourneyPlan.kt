package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A weekly journey plan (PJP) row mapping a rep's beat to a weekday. `/sfa/v1/journey-plans/sync`. */
@Serializable
data class JourneyPlan(
    val uid: String = "",
    @SerialName("rep_member_uid") val repMemberUid: String = "",
    @SerialName("beat_uid") val beatUid: String = "",
    val weekday: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
