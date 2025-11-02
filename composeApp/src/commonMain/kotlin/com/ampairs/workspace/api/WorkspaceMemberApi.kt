package com.ampairs.workspace.api

import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.MemberDetailsResponse
import com.ampairs.workspace.api.model.PagedMemberResponse
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.api.model.WorkspacePermissionResponse

/**
 * Workspace Member Management API
 *
 * Handles all member-related operations within workspaces including
 * viewing, updating, and removing members with role-based access control.
 */
interface WorkspaceMemberApi {

    /**
     * Get workspace members with pagination and sorting
     *
     * @param workspaceId Target workspace identifier
     * @param page Page number (0-based) for pagination
     * @param size Page size (1-100, default: 20)
     * @param sortBy Sort field (joinedAt, name, email, role, lastActivity)
     * @param sortDir Sort direction (asc, desc)
     * @return Paginated list of workspace members
     */
    suspend fun getWorkspaceMembers(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
    ): Response<PagedMemberResponse>

    /**
     * Get detailed information about a specific workspace member
     *
     * @param workspaceId Target workspace identifier
     * @param memberId Unique member identifier
     * @return Comprehensive member details including profile and permissions
     */
    suspend fun getMemberDetails(
        workspaceId: String,
        memberId: String,
    ): Response<MemberDetailsResponse>

    /**
     * Update member role and permissions
     *
     * @param workspaceId Target workspace identifier
     * @param memberId Member identifier to update
     * @param request Update member request with new role and permissions
     * @return Updated member information
     */
    suspend fun updateMember(
        workspaceId: String,
        memberId: String,
        request: UpdateMemberRequest,
    ): Response<MemberDetailsResponse>

    /**
     * Remove member from workspace
     *
     * Permanently removes a member from the workspace, revoking all access rights
     * and permissions. This operation is irreversible.
     *
     * @param workspaceId Target workspace identifier
     * @param memberId Member identifier to remove
     * @return Success message with removal details
     */
    suspend fun removeMember(
        workspaceId: String,
        memberId: String,
    ): Response<String>

    /**
     * Get current user's role and permissions in workspace
     *
     * Returns comprehensive information about the authenticated user's role,
     * permissions, and access rights within the specified workspace context.
     *
     * @param workspaceId Target workspace identifier
     * @return User's role, permissions, and access information
     */
    suspend fun getMyRole(workspaceId: String): Response<UserRoleResponse>

    /**
     * Get all available roles in the workspace
     *
     * Returns a list of all roles that can be assigned to members in this workspace.
     * The available roles may vary based on the workspace configuration and the
     * current user's permissions.
     *
     * @param workspaceId Target workspace identifier
     * @return List of available roles with their descriptions and permissions
     */
    suspend fun getAvailableRoles(workspaceId: String): Response<List<WorkspaceRole>>

    /**
     * Get all available permissions for the workspace
     *
     * Returns comprehensive permission structure including modules, actions,
     * and access levels that can be assigned to members.
     *
     * @param workspaceId Target workspace identifier
     * @return Hierarchical permission structure organized by modules
     */
    suspend fun getAvailablePermissions(workspaceId: String): Response<List<WorkspacePermissionResponse>>
}