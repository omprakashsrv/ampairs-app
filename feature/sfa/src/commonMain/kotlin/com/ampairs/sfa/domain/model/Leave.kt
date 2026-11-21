package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Manager-marked rep leave. Wire model for `/sfa/v1/leaves/sync`. */
@Serializable
data class Leave(
    val uid: String = "",
    @SerialName("rep_member_uid") val repMemberUid: String = "",
    @SerialName("leave_date") val leaveDate: String = "",
    val reason: String? = null,
    @SerialName("marked_by") val markedBy: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
