package com.ampairs.auth.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDeletionResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("deletion_requested") val deletionRequested: Boolean,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("deletion_scheduled_for") val deletionScheduledFor: String? = null,
    @SerialName("days_until_permanent_deletion") val daysUntilPermanentDeletion: Int? = null,
    val message: String,
    @SerialName("can_restore") val canRestore: Boolean,
    @SerialName("blocking_workspaces") val blockingWorkspaces: List<BlockingWorkspace>? = null
)
