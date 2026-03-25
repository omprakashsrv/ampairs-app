package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import com.ampairs.workspace.api.WorkspaceInvitationApi
import com.ampairs.workspace.api.model.AcceptInvitationResponse
import com.ampairs.workspace.api.model.CreateInvitationRequest
import com.ampairs.workspace.api.model.ResendInvitationRequest
import com.ampairs.workspace.domain.WorkspaceInvitation
import com.ampairs.workspace.domain.InvitationAcceptanceResult

/**
 * Repository for workspace invitation management
 *
 * Handles invitation-related operations including creation, acceptance,
 * tracking, and management with proper data transformation and error handling.
 */
class WorkspaceInvitationRepository(
    private val invitationApi: WorkspaceInvitationApi,
    private val tokenRepository: TokenRepository,
) {

    /**
     * Get workspace invitations with pagination
     */
    suspend fun getWorkspaceInvitations(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
    ): PageResult<WorkspaceInvitation> {
        val response = invitationApi.getWorkspaceInvitations(workspaceId, page, size, sortBy, sortDir)

        return if (response.error == null && response.data != null) {
            val pagedResponse = response.data!!

            val invitations = pagedResponse.content.map { invitationItem ->
                WorkspaceInvitation(
                    id = invitationItem.id,
                    workspaceId = "", // Backend list response doesn't include workspace_id
                    countryCode = invitationItem.countryCode ?: 0,
                    phone = invitationItem.phone ?: "",
                    recipientName = invitationItem.email, // Use email as name fallback
                    invitedRole = invitationItem.role,
                    status = invitationItem.status,
                    createdAt = invitationItem.createdAt,
                    expiresAt = invitationItem.expiresAt,
                    sentByName = invitationItem.inviterName ?: "Unknown",
                    sentByEmail = invitationItem.invitedBy ?: "Unknown",
                    emailSent = true, // Backend doesn't provide detailed delivery status
                    emailDelivered = true,
                    emailOpened = false,
                    linkClicked = false,
                    resendCount = invitationItem.sendCount
                )
            }

            PageResult(
                content = invitations,
                totalElements = pagedResponse.totalElements,
                totalPages = pagedResponse.totalPages,
                currentPage = pagedResponse.page,
                pageSize = pagedResponse.size,
                isFirst = pagedResponse.isFirst,
                isLast = pagedResponse.isLast,
                isEmpty = invitations.isEmpty()
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to fetch workspace invitations")
        }
    }

    /**
     * Create and send workspace invitation
     */
    suspend fun createInvitation(
        workspaceId: String,
        request: CreateInvitationRequest,
    ): WorkspaceInvitation {
        val response = invitationApi.createInvitation(workspaceId, request)

        return if (response.error == null && response.data != null) {
            val invitationData = response.data!!

            WorkspaceInvitation(
                id = invitationData.id,
                workspaceId = invitationData.workspaceId,
                countryCode = invitationData.countryCode ?: 0,
                phone = invitationData.phone ?: "",
                recipientName = invitationData.email, // Use email as name fallback
                invitedRole = invitationData.role,
                status = invitationData.status,
                createdAt = invitationData.createdAt,
                expiresAt = invitationData.expiresAt,
                sentByName = invitationData.inviterName ?: "Unknown",
                sentByEmail = invitationData.invitedBy ?: "Unknown",
                emailSent = true, // Backend doesn't provide detailed delivery status
                emailDelivered = true,
                emailOpened = false,
                linkClicked = false,
                resendCount = invitationData.sendCount,
                invitationMessage = invitationData.message
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to create invitation")
        }
    }

    /**
     * Accept workspace invitation via token
     */
    suspend fun acceptInvitation(token: String): InvitationAcceptanceResult {
        val response = invitationApi.acceptInvitation(token)

        return if (response.error == null && response.data != null) {
            val acceptanceData = response.data!!

            InvitationAcceptanceResult(
                invitationId = acceptanceData.id,
                status = acceptanceData.status,
                acceptedAt = acceptanceData.acceptedAt,
                workspaceId = acceptanceData.workspaceInfo.id,
                workspaceName = acceptanceData.workspaceInfo.name,
                workspaceDescription = acceptanceData.workspaceInfo.description,
                memberCount = acceptanceData.workspaceInfo.memberCount,
                yourRole = acceptanceData.workspaceInfo.yourRole,
                memberId = acceptanceData.memberDetails.memberId,
                userId = acceptanceData.memberDetails.userId,
                email = acceptanceData.memberDetails.email,
                name = acceptanceData.memberDetails.name,
                role = acceptanceData.memberDetails.role,
                memberStatus = acceptanceData.memberDetails.status,
                joinedAt = acceptanceData.memberDetails.joinedAt,
                permissions = acceptanceData.memberDetails.permissions,
                welcomeTourAvailable = acceptanceData.onboarding.welcomeTourAvailable,
                profileCompletionRequired = acceptanceData.onboarding.profileCompletionRequired,
                setupTasks = acceptanceData.onboarding.setupTasks,
                welcomeMessage = acceptanceData.onboarding.welcomeMessage,
                dashboardUrl = acceptanceData.immediateAccess.dashboardUrl,
                availableModules = acceptanceData.immediateAccess.availableModules,
                teamMembers = acceptanceData.immediateAccess.teamMembers,
                recentActivityAvailable = acceptanceData.immediateAccess.recentActivityAvailable
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to accept invitation")
        }
    }

    /**
     * Resend invitation email
     */
    suspend fun resendInvitation(
        workspaceId: String,
        invitationId: String,
        request: ResendInvitationRequest = ResendInvitationRequest(),
    ): WorkspaceInvitation {
        val response = invitationApi.resendInvitation(workspaceId, invitationId, request)

        return if (response.error == null && response.data != null) {
            val invitationData = response.data!!

            WorkspaceInvitation(
                id = invitationData.id,
                workspaceId = invitationData.workspaceId,
                countryCode = invitationData.countryCode ?: 0,
                phone = invitationData.phone ?: "",
                recipientName = invitationData.email, // Use email as name fallback
                invitedRole = invitationData.role,
                status = invitationData.status,
                createdAt = invitationData.createdAt,
                expiresAt = invitationData.expiresAt,
                sentByName = invitationData.inviterName ?: "Unknown",
                sentByEmail = invitationData.invitedBy ?: "Unknown",
                emailSent = true, // Backend doesn't provide detailed delivery status
                emailDelivered = true,
                emailOpened = false,
                linkClicked = false,
                resendCount = invitationData.sendCount,
                invitationMessage = invitationData.message
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to resend invitation")
        }
    }

    /**
     * Cancel workspace invitation
     */
    suspend fun cancelInvitation(workspaceId: String, invitationId: String): String {
        val response = invitationApi.cancelInvitation(workspaceId, invitationId)

        return if (response.error == null && response.data != null) {
            response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to cancel invitation")
        }
    }
}