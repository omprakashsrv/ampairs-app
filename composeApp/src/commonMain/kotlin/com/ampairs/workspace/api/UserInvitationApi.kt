package com.ampairs.workspace.api

import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.UserInvitationResponse
import com.ampairs.workspace.api.model.InvitationActionResponse

/**
 * User Invitation Management API
 *
 * User-scoped invitation management endpoints for pending workspace invitations.
 * These endpoints work across all workspaces and don't require workspace context.
 */
interface UserInvitationApi {

    /**
     * Get current user's pending workspace invitations
     *
     * Retrieves all pending workspace invitations for the currently authenticated user.
     * This endpoint allows users to see all workspaces they've been invited to join.
     *
     * @return List of pending invitations with workspace details
     */
    suspend fun getPendingInvitations(): Response<List<UserInvitationResponse>>

    /**
     * Accept workspace invitation
     *
     * Accepts a pending workspace invitation, adding the user to the workspace
     * with the pre-assigned role and permissions.
     *
     * @param invitationId The invitation ID to accept
     * @return Acceptance confirmation with workspace and member details
     */
    suspend fun acceptInvitation(invitationId: String): Response<InvitationActionResponse>

    /**
     * Reject workspace invitation
     *
     * Rejects a pending workspace invitation, removing it from the user's
     * pending invitations list.
     *
     * @param invitationId The invitation ID to reject
     * @return Rejection confirmation
     */
    suspend fun rejectInvitation(invitationId: String): Response<InvitationActionResponse>
}