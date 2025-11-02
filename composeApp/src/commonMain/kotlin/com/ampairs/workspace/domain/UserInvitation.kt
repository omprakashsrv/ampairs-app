package com.ampairs.workspace.domain

/**
 * Domain model for user workspace invitations
 */
data class UserInvitation(
    val id: String,
    val workspaceId: String,
    val workspaceName: String,
    val workspaceDescription: String? = null,
    val workspaceType: String? = null,
    val role: String,
    val message: String? = null,
    val invitedBy: String? = null,
    val inviterName: String? = null,
    val expiresAt: String,
    val createdAt: String,
    val daysUntilExpiry: Long? = null,
)