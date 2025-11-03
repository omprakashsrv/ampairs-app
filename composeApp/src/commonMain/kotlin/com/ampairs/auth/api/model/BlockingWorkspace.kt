package com.ampairs.auth.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlockingWorkspace(
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("workspace_name") val workspaceName: String,
    @SerialName("workspace_slug") val workspaceSlug: String,
    @SerialName("member_count") val memberCount: Int
)
