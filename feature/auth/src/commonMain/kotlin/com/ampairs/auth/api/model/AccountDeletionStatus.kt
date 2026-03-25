package com.ampairs.auth.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDeletionStatus(
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("deletion_scheduled_for") val deletionScheduledFor: String? = null,
    @SerialName("days_remaining") val daysRemaining: Int? = null,
    @SerialName("can_restore") val canRestore: Boolean,
    @SerialName("deletion_reason") val deletionReason: String? = null,
    @SerialName("status_message") val statusMessage: String
)
