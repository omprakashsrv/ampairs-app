package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A store visit captured offline by the rep. Wire model for `/sfa/v1/visits/sync`. */
@Serializable
data class Visit(
    val uid: String = "",
    @SerialName("customer_uid") val customerUid: String = "",
    @SerialName("rep_member_uid") val repMemberUid: String = "",
    @SerialName("planned_visit_uid") val plannedVisitUid: String? = null,
    /** PRODUCTIVE / NO_ORDER / OUTLET_CLOSED (server enum, sent as its name). */
    val outcome: String = "PRODUCTIVE",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("distance_meters") val distanceMeters: Double? = null,
    /** IN_RADIUS / OUT_OF_RADIUS / NO_LOCATION. */
    @SerialName("geo_fence_status") val geoFenceStatus: String = "NO_LOCATION",
    @SerialName("ad_hoc") val adHoc: Boolean = false,
    val notes: String? = null,
    @SerialName("order_uid") val orderUid: String? = null,
    @SerialName("visited_at") val visitedAt: String = "",
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
