package com.ampairs.workspace.api

import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.AcceptInvitationResponse
import com.ampairs.workspace.api.model.CreateInvitationRequest
import com.ampairs.workspace.api.model.InvitationApiModel
import com.ampairs.workspace.api.model.PagedInvitationResponse
import com.ampairs.workspace.api.model.ResendInvitationRequest

/**
 * Workspace Invitation Management API
 *
 * Comprehensive invitation system for workspace collaboration including
 * invitation creation, acceptance, tracking, and management.
 */
interface WorkspaceInvitationApi {

    /**
     * Get workspace invitations with pagination and sorting
     *
     * Fetches a comprehensive list of all workspace invitations with their status,
     * recipient information, and tracking details for invitation management and monitoring.
     *
     * @param workspaceId Target workspace identifier
     * @param page Page number (0-based) for pagination
     * @param size Page size (1-100, default: 20)
     * @param sortBy Sort field (createdAt, status, recipientEmail, role, expiresAt)
     * @param sortDir Sort direction (asc, desc)
     * @return Paginated list of workspace invitations
     */
    suspend fun getWorkspaceInvitations(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
    ): Response<PagedInvitationResponse>

    /**
     * Create and send workspace invitation
     *
     * Creates and sends a personalized invitation to join the workspace with specified
     * role and permissions. The invitation includes secure token-based access and
     * comprehensive onboarding information.
     *
     * @param workspaceId Target workspace identifier where the invitee will become a member
     * @param request Comprehensive invitation creation details
     * @return Created invitation details with token and delivery information
     */
    suspend fun createInvitation(
        workspaceId: String,
        request: CreateInvitationRequest,
    ): Response<InvitationApiModel>

    /**
     * Accept workspace invitation via token
     *
     * Public endpoint for accepting workspace invitations using the secure invitation token.
     * This completes the invitation workflow by adding the user to the workspace with
     * the pre-assigned role and permissions.
     *
     * @param token Secure invitation token from invitation email
     * @return Acceptance confirmation with workspace and member details
     */
    suspend fun acceptInvitation(token: String): Response<AcceptInvitationResponse>

    /**
     * Resend invitation email
     *
     * Resends an existing workspace invitation with updated delivery options,
     * custom messaging, and tracking. Useful for following up on pending invitations
     * or addressing delivery issues.
     *
     * @param workspaceId Target workspace identifier containing the invitation to resend
     * @param invitationId Invitation identifier to resend
     * @param request Optional resend configuration options
     * @return Updated invitation details with new delivery information
     */
    suspend fun resendInvitation(
        workspaceId: String,
        invitationId: String,
        request: ResendInvitationRequest = ResendInvitationRequest(),
    ): Response<InvitationApiModel>

    /**
     * Cancel workspace invitation
     *
     * Permanently cancels a pending workspace invitation, preventing its acceptance
     * and removing it from active invitation lists. This is irreversible and will
     * invalidate the invitation token.
     *
     * @param workspaceId Target workspace identifier containing the invitation to cancel
     * @param invitationId Invitation identifier to cancel permanently
     * @return Success message with cancellation details
     */
    suspend fun cancelInvitation(
        workspaceId: String,
        invitationId: String,
    ): Response<String>
}