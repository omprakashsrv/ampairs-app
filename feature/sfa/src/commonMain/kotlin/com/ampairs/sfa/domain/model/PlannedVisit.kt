package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A scheduled visit derived from a journey plan. Wire model for `/sfa/v1/planned-visits/sync`. */
@Serializable
data class PlannedVisit(
    val uid: String = "",
    @SerialName("journey_plan_uid") val journeyPlanUid: String? = null,
    @SerialName("beat_uid") val beatUid: String? = null,
    @SerialName("customer_uid") val customerUid: String = "",
    @SerialName("rep_member_uid") val repMemberUid: String = "",
    @SerialName("planned_date") val plannedDate: String = "",
    /** PENDING / VISITED / MISSED. */
    val status: String = "PENDING",
    @SerialName("visit_sequence") val visitSequence: Int = 0,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
