package com.ampairs.workspace.domain

import com.ampairs.workspace.api.model.WorkspaceApiModel
import com.ampairs.workspace.api.model.WorkspaceListApiModel
import com.ampairs.workspace.db.entity.WorkspaceEntity
import com.ampairs.workspace.db.entity.WorkspaceMemberEntity
import kotlinx.serialization.json.Json

data class Workspace(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val workspaceType: String = "BUSINESS",
    val avatarUrl: String? = null,
    val isActive: Boolean = true,
    val subscriptionPlan: String = "FREE",
    val maxMembers: Int = 5,
    val storageLimitGb: Int = 1,
    val storageUsedGb: Int = 0,
    val timezone: String = "UTC",
    val language: String = "en",
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val lastActivityAt: String? = null,
    val trialExpiresAt: String? = null,
    val memberCount: Int? = null,
    val isTrial: Boolean? = null,
    val storagePercentage: Double? = null,
)

data class WorkspaceList(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val workspaceType: String = "BUSINESS",
    val avatarUrl: String? = null,
    val subscriptionPlan: String = "FREE",
    val memberCount: Int = 1,
    val lastActivityAt: String? = null,
    val createdAt: String,
)


// Extension functions for converting between different model types

fun WorkspaceApiModel.asDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt,
        trialExpiresAt = this.trialExpiresAt,
        memberCount = this.memberCount,
        isTrial = this.isTrial,
        storagePercentage = this.storagePercentage,
    )
}

fun WorkspaceListApiModel.asDomainModel(): WorkspaceList {
    return WorkspaceList(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        subscriptionPlan = this.subscriptionPlan,
        memberCount = this.memberCount,
        lastActivityAt = this.lastActivityAt,
        createdAt = this.createdAt,
    )
}

fun Workspace.asDatabaseModel(): WorkspaceEntity {
    return WorkspaceEntity(
        seq_id = 0,
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description ?: "",
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl ?: "",
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt ?: "",
        trialExpiresAt = this.trialExpiresAt ?: "",
        memberCount = this.memberCount ?: 0,
        isTrial = this.isTrial ?: false,
        storagePercentage = this.storagePercentage ?: 0.0,
    )
}

fun WorkspaceEntity.asDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = if (this.description.isEmpty()) null else this.description,
        workspaceType = this.workspaceType,
        avatarUrl = if (this.avatarUrl.isEmpty()) null else this.avatarUrl,
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = if (this.lastActivityAt.isEmpty()) null else this.lastActivityAt,
        trialExpiresAt = if (this.trialExpiresAt.isEmpty()) null else this.trialExpiresAt,
        memberCount = this.memberCount,
        isTrial = this.isTrial,
        storagePercentage = this.storagePercentage,
    )
}

// ===== WORKSPACE MEMBER DOMAIN MODELS =====

/**
 * Domain model representing a workspace member
 */
data class WorkspaceMember(
    val id: String,
    val userId: String,
    val workspaceId: String,
    val name: String,
    val email: String?,
    val phone: String? = null,
    val role: String,
    val status: String,
    val joinedAt: String,
    val lastActivity: String? = null,
    val permissions: List<String> = emptyList(),
    val avatarUrl: String? = null,
)

// ===== WORKSPACE INVITATION DOMAIN MODELS =====

/**
 * Domain model representing a workspace invitation
 */
data class WorkspaceInvitation(
    val id: String,
    val workspaceId: String,
    val countryCode: Int,
    val phone: String,
    val recipientName: String? = null,
    val invitedRole: String,
    val status: String,
    val createdAt: String,
    val expiresAt: String,
    val sentByName: String,
    val sentByEmail: String,
    val emailSent: Boolean = false,
    val emailDelivered: Boolean = false,
    val emailOpened: Boolean = false,
    val linkClicked: Boolean = false,
    val resendCount: Int = 0,
    val invitationMessage: String? = null,
)

/**
 * Domain model for invitation acceptance result
 */
data class InvitationAcceptanceResult(
    val invitationId: String,
    val status: String,
    val acceptedAt: String,
    val workspaceId: String,
    val workspaceName: String,
    val workspaceDescription: String? = null,
    val memberCount: Int,
    val yourRole: String,
    val memberId: String,
    val userId: String,
    val email: String,
    val name: String,
    val role: String,
    val memberStatus: String,
    val joinedAt: String,
    val permissions: List<String>,
    val welcomeTourAvailable: Boolean,
    val profileCompletionRequired: Boolean,
    val setupTasks: List<String>,
    val welcomeMessage: String? = null,
    val dashboardUrl: String,
    val availableModules: List<String>,
    val teamMembers: Int,
    val recentActivityAvailable: Boolean,
)

// ===== WORKSPACE MEMBER EXTENSION FUNCTIONS =====

fun WorkspaceMember.asDatabaseModel(): WorkspaceMemberEntity {
    return WorkspaceMemberEntity(
        seq_id = 0,
        id = this.id,
        user_id = "", // Will be set by repository with current user ID
        member_user_id = this.userId,
        workspace_id = this.workspaceId,
        email = this.email ?: "",
        name = this.name,
        role = this.role,
        status = this.status,
        joined_at = this.joinedAt,
        last_activity = this.lastActivity,
        permissions = if (this.permissions.isEmpty()) "[]" else kotlinx.serialization.json.Json.encodeToString(this.permissions),
        avatar_url = this.avatarUrl,
        phone = this.phone,
    )
}

fun WorkspaceMemberEntity.asDomainModel(): WorkspaceMember {
    return WorkspaceMember(
        id = this.id,
        userId = this.member_user_id,
        workspaceId = this.workspace_id,
        email = if (this.email.isEmpty()) null else this.email,
        name = this.name,
        role = this.role,
        status = this.status,
        joinedAt = this.joined_at,
        lastActivity = this.last_activity,
        permissions = try {
            if (this.permissions.isEmpty() || this.permissions == "{}" || this.permissions == "[]") {
                emptyList()
            } else {
                // Parse permissions from JSON string stored in database
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(this.permissions)
            }
        } catch (e: Exception) {
            emptyList()
        },
        avatarUrl = this.avatar_url,
        phone = this.phone,
    )
}