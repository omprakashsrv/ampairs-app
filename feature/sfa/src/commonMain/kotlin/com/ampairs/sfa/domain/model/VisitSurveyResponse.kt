package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Offline-captured store-visit survey answers (JSON blob keyed by question). `/sfa/v1/visit-surveys/sync`. */
@Serializable
data class VisitSurveyResponse(
    val uid: String = "",
    @SerialName("visit_uid") val visitUid: String = "",
    @SerialName("rep_member_uid") val repMemberUid: String? = null,
    val responses: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
