package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A rep's check-in/check-out attendance record. Wire model for `/sfa/v1/attendance/sync`. */
@Serializable
data class Attendance(
    val uid: String = "",
    @SerialName("rep_member_uid") val repMemberUid: String = "",
    @SerialName("check_in_at") val checkInAt: String? = null,
    @SerialName("check_in_latitude") val checkInLatitude: Double? = null,
    @SerialName("check_in_longitude") val checkInLongitude: Double? = null,
    @SerialName("check_out_at") val checkOutAt: String? = null,
    @SerialName("check_out_latitude") val checkOutLatitude: Double? = null,
    @SerialName("check_out_longitude") val checkOutLongitude: Double? = null,
    /** OPEN / CLOSED / AUTO_CLOSED. */
    val status: String = "OPEN",
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
