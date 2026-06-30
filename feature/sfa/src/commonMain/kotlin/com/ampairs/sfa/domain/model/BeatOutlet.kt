package com.ampairs.sfa.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An outlet's position on a beat. Wire model for `/sfa/v1/beat-outlets/sync`. */
@Serializable
data class BeatOutlet(
    val uid: String = "",
    @SerialName("beat_uid") val beatUid: String = "",
    @SerialName("customer_uid") val customerUid: String = "",
    @SerialName("visit_sequence") val visitSequence: Int = 0,
    @SerialName("visit_day") val visitDay: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
