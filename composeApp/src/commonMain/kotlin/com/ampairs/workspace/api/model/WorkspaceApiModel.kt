package com.ampairs.workspace.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===== CORE WORKSPACE MODELS =====

@Serializable
data class WorkspaceApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("subscription_plan") val subscriptionPlan: String = "FREE",
    @SerialName("max_members") val maxMembers: Int = 5,
    @SerialName("storage_limit_gb") val storageLimitGb: Int = 1,
    @SerialName("storage_used_gb") val storageUsedGb: Int = 0,
    @SerialName("timezone") val timezone: String = "UTC",
    @SerialName("language") val language: String = "en",
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
    @SerialName("trial_expires_at") val trialExpiresAt: String? = null,
    @SerialName("member_count") val memberCount: Int? = null,
    @SerialName("is_trial") val isTrial: Boolean? = null,
    @SerialName("storage_percentage") val storagePercentage: Double? = null,
)

@Serializable
data class WorkspaceListApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("subscription_plan") val subscriptionPlan: String = "FREE",
    @SerialName("member_count") val memberCount: Int = 1,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class CreateWorkspaceRequest(
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("timezone") val timezone: String = "UTC",
    @SerialName("language") val language: String = "en",
)

@Serializable
data class UpdateWorkspaceRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("language") val language: String? = null,
)


@Serializable
data class PagedWorkspaceResponse(
    @SerialName("content") val content: List<WorkspaceListApiModel>,
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("total_elements") val totalElements: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("first") val first: Boolean,
    @SerialName("last") val last: Boolean,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_previous") val hasPrevious: Boolean,
    @SerialName("empty") val empty: Boolean,
)

// ===== WORKSPACE MEMBER MANAGEMENT MODELS =====

@Serializable
data class UserApiModel(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("profile_picture_url") val profilePictureUrl: String? = null,
) {
    fun getDisplayName(): String {
        return when {
            firstName.isNotBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            !email.isNullOrBlank() -> email
            !phone.isNullOrBlank() -> phone
            else -> id
        }
    }
}

@Serializable
data class MemberApiModel(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class MemberDetailsResponse(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("workspace_id") val workspaceId: String,

    // Flattened user fields from backend
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,

    // Nested user object
    @SerialName("user") val user: UserApiModel? = null,

    @SerialName("role") val role: String,
    @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
    @SerialName("invitation_accepted_at") val invitationAcceptedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,

    // Team information
    @SerialName("primary_team") val primaryTeam: TeamSummaryApiModel? = null,
    @SerialName("teams") val teams: List<TeamSummaryApiModel> = emptyList(),
    @SerialName("job_title") val jobTitle: String? = null,
) {
    // Computed properties to match the expected interface
    val name: String
        get() = user?.getDisplayName()
            ?: listOfNotNull(firstName, lastName).joinToString(" ").takeIf { it.isNotBlank() }
            ?: email
            ?: userId

    val status: String get() = if (isActive) "ACTIVE" else "INACTIVE"
}

@Serializable
data class TeamSummaryApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("team_code") val teamCode: String,
    @SerialName("department") val department: String? = null,
    @SerialName("is_primary_team") val isPrimaryTeam: Boolean = false,
)

@Serializable
data class MemberListResponse(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user") val user: UserApiModel? = null,
    @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerialName("role") val role: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
)

@Serializable
data class UpdateMemberRequest(
    @SerialName("role") val role: String? = null,
    @SerialName("permissions") val permissions: Set<WorkspacePermission>? = null,
    @SerialName("reason") val reason: String? = null,
    @SerialName("notify_member") val notifyMember: Boolean = true,
)

@Serializable
data class PagedMemberResponse(
    @SerialName("content") val content: List<MemberListResponse>,
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("total_elements") val totalElements: Long,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("first") val first: Boolean,
    @SerialName("last") val last: Boolean,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_previous") val hasPrevious: Boolean,
    @SerialName("empty") val empty: Boolean,
)

@Serializable
data class UserRoleResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("current_role") val currentRole: String,
    @SerialName("membership_status") val membershipStatus: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("role_hierarchy") val roleHierarchy: Map<String, Boolean>,
    @SerialName("permissions") val permissions: Map<String, Map<String, Boolean>>,
    @SerialName("module_access") val moduleAccess: List<String>,
)

// ===== USER INVITATION MODELS =====

@Serializable
data class UserInvitationResponse(
    @SerialName("id") val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("workspace_name") val workspaceName: String? = null,
    @SerialName("workspace_description") val workspaceDescription: String? = null,
    @SerialName("workspace_type") val workspaceType: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("country_code") val countryCode: Int? = null,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("invited_by") val invitedBy: String? = null,
    @SerialName("inviter_name") val inviterName: String? = null,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("rejected_at") val rejectedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("cancelled_by") val cancelledBy: String? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("send_count") val sendCount: Int? = null,
    @SerialName("last_sent_at") val lastSentAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_expired") val isExpired: Boolean? = null,
    @SerialName("days_until_expiry") val daysUntilExpiry: Long? = null,
)

@Serializable
data class InvitationActionResponse(
    @SerialName("success") val success: Boolean? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("invitation_id") val invitationId: String? = null,
    @SerialName("workspace_id") val workspaceId: String? = null,
    @SerialName("workspace_name") val workspaceName: String? = null,
    @SerialName("member_id") val memberId: String? = null,
    @SerialName("action") val action: String? = null, // "accept" or "reject"
    @SerialName("processed_at") val processedAt: String? = null,
)

// ===== WORKSPACE INVITATION MANAGEMENT MODELS =====

@Serializable
data class InvitationApiModel(
    @SerialName("id") val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("workspace_name") val workspaceName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("country_code") val countryCode: Int? = null,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("token") val token: String,
    @SerialName("message") val message: String? = null,
    @SerialName("invited_by") val invitedBy: String? = null,
    @SerialName("inviter_name") val inviterName: String? = null,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("rejected_at") val rejectedAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("cancelled_by") val cancelledBy: String? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("send_count") val sendCount: Int = 0,
    @SerialName("last_sent_at") val lastSentAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_expired") val isExpired: Boolean,
    @SerialName("days_until_expiry") val daysUntilExpiry: Long? = null,
)

@Serializable
data class InvitationListResponse(
    @SerialName("id") val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("country_code") val countryCode: Int? = null,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("invited_by") val invitedBy: String? = null,
    @SerialName("inviter_name") val inviterName: String? = null,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("invited_at") val invitedAt: String,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("send_count") val sendCount: Int = 0,
    @SerialName("last_sent_at") val lastSentAt: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_expired") val isExpired: Boolean,
)


@Serializable
data class CreateInvitationRequest(
    @SerialName("country_code") val countryCode: Int,
    @SerialName("phone") val phone: String,
    @SerialName("name") val name: String? = null,
    @SerialName("invited_role") val invitedRole: String,
    @SerialName("custom_message") val customMessage: String? = null,
    @SerialName("expires_in_days") val expiresInDays: Int = 7,
    @SerialName("send_notification") val sendNotification: Boolean = true,
    @SerialName("permissions") val permissions: List<String>? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("welcome_tour") val welcomeTour: Boolean = true,
)

@Serializable
data class ResendInvitationRequest(
    @SerialName("updated_message") val updatedMessage: String? = null,
    @SerialName("priority_delivery") val priorityDelivery: Boolean = false,
    @SerialName("send_time") val sendTime: String? = null,
    @SerialName("include_reminder") val includeReminder: Boolean = true,
    @SerialName("extend_expiration_days") val extendExpirationDays: Int? = null,
)

@Serializable
data class PagedInvitationResponse(
    @SerialName("content") val content: List<InvitationListResponse> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("size") val size: Int = 20,
    @SerialName("total_elements") val totalElements: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("is_first") val isFirst: Boolean = true,
    @SerialName("is_last") val isLast: Boolean = true,
)

@Serializable
data class AcceptInvitationResponse(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String,
    @SerialName("accepted_at") val acceptedAt: String,
    @SerialName("workspace_info") val workspaceInfo: AcceptedWorkspaceInfo,
    @SerialName("member_details") val memberDetails: AcceptedMemberDetails,
    @SerialName("onboarding") val onboarding: OnboardingInfo,
    @SerialName("immediate_access") val immediateAccess: ImmediateAccessInfo,
)

@Serializable
data class AcceptedWorkspaceInfo(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("your_role") val yourRole: String,
)

@Serializable
data class AcceptedMemberDetails(
    @SerialName("member_id") val memberId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("permissions") val permissions: List<String>,
)

@Serializable
data class OnboardingInfo(
    @SerialName("welcome_tour_available") val welcomeTourAvailable: Boolean,
    @SerialName("profile_completion_required") val profileCompletionRequired: Boolean,
    @SerialName("setup_tasks") val setupTasks: List<String>,
    @SerialName("welcome_message") val welcomeMessage: String? = null,
)

@Serializable
data class ImmediateAccessInfo(
    @SerialName("dashboard_url") val dashboardUrl: String,
    @SerialName("available_modules") val availableModules: List<String>,
    @SerialName("team_members") val teamMembers: Int,
    @SerialName("recent_activity_available") val recentActivityAvailable: Boolean,
)

// ===== ROLES AND PERMISSIONS MODELS =====

@Serializable
data class WorkspaceRole(
    @SerialName("name") val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("level") val level: Int,
    @SerialName("description") val description: String,
    @SerialName("manageable_roles") val manageableRoles: List<String>
)

@Serializable
data class WorkspacePermissionResponse(
    @SerialName("name") val name: String,
    @SerialName("permission_name") val permissionName: String,
    @SerialName("description") val description: String
)

/**
 * Client-side WorkspacePermission enum matching backend enum
 */
@Serializable
enum class WorkspacePermission(val permissionName: String) {
    @SerialName("WORKSPACE_MANAGE")
    WORKSPACE_MANAGE("WORKSPACE_MANAGE"),
    @SerialName("WORKSPACE_DELETE")
    WORKSPACE_DELETE("WORKSPACE_DELETE"),
    @SerialName("MEMBER_VIEW")
    MEMBER_VIEW("MEMBER_VIEW"),
    @SerialName("MEMBER_INVITE")
    MEMBER_INVITE("MEMBER_INVITE"),
    @SerialName("MEMBER_MANAGE")
    MEMBER_MANAGE("MEMBER_MANAGE"),
    @SerialName("MEMBER_DELETE")
    MEMBER_DELETE("MEMBER_DELETE");

    override fun toString(): String = permissionName

    companion object {
        fun fromString(permission: String): WorkspacePermission? {
            return entries.find { it.permissionName == permission }
        }
    }
}