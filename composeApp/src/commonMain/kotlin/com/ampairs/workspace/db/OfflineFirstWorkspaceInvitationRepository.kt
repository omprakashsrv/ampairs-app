package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceInvitationApi
import com.ampairs.workspace.api.model.CreateInvitationRequest
import com.ampairs.workspace.api.model.ResendInvitationRequest
import com.ampairs.workspace.db.dao.WorkspaceInvitationDao
import com.ampairs.workspace.domain.InvitationAcceptanceResult
import com.ampairs.workspace.domain.WorkspaceInvitation
import com.ampairs.workspace.store.WorkspaceInvitationKey
import com.ampairs.workspace.store.WorkspaceInvitationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Offline-first workspace invitation repository using Store5 pattern
 * 
 * Provides comprehensive invitation management with proper offline-first functionality,
 * automatic sync, conflict resolution, and optimistic updates.
 */
@OptIn(ExperimentalStoreApi::class)
class OfflineFirstWorkspaceInvitationRepository(
    private val invitationApi: WorkspaceInvitationApi,
    private val invitationDao: WorkspaceInvitationDao,
    private val invitationStore: WorkspaceInvitationStore,
    private val tokenRepository: TokenRepository
) {

    private suspend fun getCurrentUserId(): String {
        return tokenRepository.getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
    }

    /**
     * Get workspace invitations with offline-first approach using Store5
     */
    suspend fun getWorkspaceInvitations(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
        refresh: Boolean = false
    ): Flow<PageResult<WorkspaceInvitation>> {
        val currentUserId = getCurrentUserId()
        println("🏪 Repository: Getting invitations for workspace $workspaceId, user $currentUserId (refresh=$refresh)")
        
        val key = WorkspaceInvitationKey.forPage(
            userId = currentUserId,
            workspaceId = workspaceId,
            page = page,
            size = size
        ).copy(sortBy = sortBy, sortDir = sortDir)
        
        val request = if (refresh) {
            StoreReadRequest.fresh(key)
        } else {
            StoreReadRequest.cached(key, refresh = false)
        }
        
        return invitationStore.stream(request).map { response ->
            println("🏪 Repository: Store5 response type: ${response::class.simpleName}")
            when (response) {
                is StoreReadResponse.Data -> {
                    println("✅ Repository: Got data with ${response.value.content.size} invitations")
                    response.value
                }
                is StoreReadResponse.Error.Exception -> {
                    println("❌ Repository: Store5 exception: ${response.error.message}")
                    throw response.error
                }
                is StoreReadResponse.Error.Message -> {
                    println("❌ Repository: Store5 error message: ${response.message}")
                    throw Exception(response.message)
                }
                is StoreReadResponse.Loading -> {
                    println("⏳ Repository: Loading state - returning empty while loading")
                    // For loading state, return empty data while loading
                    // The Store5 will eventually provide the actual data
                    PageResult(
                        content = emptyList(),
                        totalElements = 0,
                        totalPages = 0,
                        currentPage = page,
                        pageSize = size,
                        isFirst = true,
                        isLast = true,
                        isEmpty = true
                    )
                }
                is StoreReadResponse.NoNewData -> {
                    println("📦 Repository: NoNewData - this indicates cached data exists")
                    // NoNewData means we have cached data but no new network data
                    // This should not throw an exception but return cached data
                    // However, the Store5 should have provided the cached data
                    throw Exception("No cached data available in NoNewData response")
                }
                else -> {
                    println("❓ Repository: Unknown Store response type: ${response::class.simpleName}")
                    throw Exception("Unknown Store response type: ${response::class.simpleName}")
                }
            }
        }
    }

    /**
     * Get filtered workspace invitations
     */
    suspend fun getFilteredInvitations(
        workspaceId: String,
        status: String? = null,
        role: String? = null,
        page: Int = 0,
        size: Int = 20,
        refresh: Boolean = false
    ): Flow<PageResult<WorkspaceInvitation>> {
        val currentUserId = getCurrentUserId()
        return invitationStore.stream(
            StoreReadRequest.cached(
                key = WorkspaceInvitationKey.forPageWithFilters(
                    userId = currentUserId,
                    workspaceId = workspaceId,
                    page = page,
                    size = size,
                    status = status,
                    role = role
                ),
                refresh = refresh
            )
        ).map { response ->
            when (response) {
                is StoreReadResponse.Data -> response.value
                is StoreReadResponse.Error.Exception -> throw response.error
                is StoreReadResponse.Error.Message -> throw Exception(response.message)
                else -> PageResult(
                    content = emptyList(),
                    totalElements = 0,
                    totalPages = 0,
                    currentPage = page,
                    pageSize = size,
                    isFirst = true,
                    isLast = true,
                    isEmpty = true
                )
            }
        }
    }

    /**
     * Create and send workspace invitation with optimistic updates
     */
    suspend fun createInvitation(
        workspaceId: String,
        request: CreateInvitationRequest,
    ): WorkspaceInvitation {
        // Make API call first
        val response = invitationApi.createInvitation(workspaceId, request)

        if (response.error == null && response.data != null) {
            val invitationData = response.data!!
            val invitation = WorkspaceInvitation(
                id = invitationData.id,
                workspaceId = invitationData.workspaceId,
                countryCode = invitationData.countryCode ?: 0,
                phone = invitationData.phone ?: "",
                recipientName = invitationData.email,
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

            // Update Store5 cache by clearing it so next read will fetch fresh data
            invitationStore.clear()

            return invitation
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
     * Resend invitation email with optimistic updates
     */
    suspend fun resendInvitation(
        workspaceId: String,
        invitationId: String,
        request: ResendInvitationRequest = ResendInvitationRequest(),
    ): WorkspaceInvitation {
        val currentUserId = getCurrentUserId()
        
        // Optimistic update - increment resend count locally first
        invitationDao.incrementResendCount(invitationId, currentUserId)
        
        try {
            val response = invitationApi.resendInvitation(workspaceId, invitationId, request)

            if (response.error == null && response.data != null) {
                val invitationData = response.data!!
                val invitation = WorkspaceInvitation(
                    id = invitationData.id,
                    workspaceId = invitationData.workspaceId,
                    countryCode = invitationData.countryCode ?: 0,
                    phone = invitationData.phone ?: "",
                    recipientName = invitationData.email,
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

                // Update local database with server data
                val currentTime = currentTimeMillis()
                val entity = invitation.toEntityModel(currentUserId).copy(
                    sync_state = "SYNCED",
                    last_synced_at = currentTime,
                    server_updated_at = currentTime,
                    local_updated_at = currentTime
                )
                invitationDao.insertInvitation(entity)

                return invitation
            } else {
                // Rollback optimistic update on failure
                // TODO: Implement proper rollback mechanism
                throw Exception(response.error?.message ?: "Failed to resend invitation")
            }
        } catch (e: Exception) {
            // TODO: Implement proper rollback mechanism
            throw e
        }
    }

    /**
     * Cancel workspace invitation with optimistic updates
     */
    suspend fun cancelInvitation(workspaceId: String, invitationId: String): String {
        val currentUserId = getCurrentUserId()
        
        // Optimistic update - change status locally first
        invitationDao.updateInvitationStatus(invitationId, currentUserId, "CANCELLED")
        
        try {
            val response = invitationApi.cancelInvitation(workspaceId, invitationId)

            if (response.error == null && response.data != null) {
                // Update sync state to confirm successful cancellation
                val currentTime = currentTimeMillis()
                invitationDao.updateSyncState(invitationId, currentUserId, "SYNCED", currentTime)
                
                return response.data!!
            } else {
                // Rollback optimistic update on failure
                // TODO: Implement proper rollback mechanism
                throw Exception(response.error?.message ?: "Failed to cancel invitation")
            }
        } catch (e: Exception) {
            // TODO: Implement proper rollback mechanism
            throw e
        }
    }

    /**
     * Search invitations locally
     */
    suspend fun searchInvitations(
        workspaceId: String,
        query: String
    ): Flow<List<WorkspaceInvitation>> {
        val currentUserId = getCurrentUserId()
        return invitationDao.searchInvitations(currentUserId, workspaceId, query)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    /**
     * Clear all cached invitations for a workspace
     */
    suspend fun clearCache(workspaceId: String) {
        val currentUserId = getCurrentUserId()
        invitationDao.deleteAllInvitationsForWorkspace(workspaceId, currentUserId)
        
        // Clear Store5 cache
        invitationStore.clear()
    }

    /**
     * Force refresh invitations from server
     */
    suspend fun refresh(workspaceId: String, page: Int = 0, size: Int = 20): PageResult<WorkspaceInvitation> {
        val currentUserId = getCurrentUserId()
        val key = WorkspaceInvitationKey.forPage(currentUserId, workspaceId, page, size)
        
        return invitationStore.stream(
            StoreReadRequest.fresh(key)
        ).first().let { response ->
            when (response) {
                is StoreReadResponse.Data -> response.value
                is StoreReadResponse.Error.Exception -> throw response.error
                is StoreReadResponse.Error.Message -> throw Exception(response.message)
                else -> throw Exception("Failed to refresh invitations")
            }
        }
    }
}


// Extension functions for entity conversions
private fun WorkspaceInvitation.toEntityModel(userId: String): com.ampairs.workspace.db.entity.WorkspaceInvitationEntity {
    return com.ampairs.workspace.db.entity.WorkspaceInvitationEntity(
        id = this.id,
        user_id = userId,
        workspace_id = this.workspaceId,
        country_code = this.countryCode,
        phone = this.phone,
        recipient_name = this.recipientName,
        invited_role = this.invitedRole,
        status = this.status,
        created_at = this.createdAt,
        expires_at = this.expiresAt,
        sent_by_name = this.sentByName,
        sent_by_email = this.sentByEmail,
        email_sent = this.emailSent,
        email_delivered = this.emailDelivered,
        email_opened = this.emailOpened,
        link_clicked = this.linkClicked,
        resend_count = this.resendCount,
        invitation_message = this.invitationMessage,
        sync_state = "SYNCED",
        last_synced_at = currentTimeMillis(),
        local_updated_at = currentTimeMillis(),
        server_updated_at = currentTimeMillis()
    )
}

private fun com.ampairs.workspace.db.entity.WorkspaceInvitationEntity.toDomainModel(): WorkspaceInvitation {
    return WorkspaceInvitation(
        id = this.id,
        workspaceId = this.workspace_id,
        countryCode = this.country_code,
        phone = this.phone,
        recipientName = this.recipient_name,
        invitedRole = this.invited_role,
        status = this.status,
        createdAt = this.created_at,
        expiresAt = this.expires_at,
        sentByName = this.sent_by_name,
        sentByEmail = this.sent_by_email,
        emailSent = this.email_sent,
        emailDelivered = this.email_delivered,
        emailOpened = this.email_opened,
        linkClicked = this.link_clicked,
        resendCount = this.resend_count,
        invitationMessage = this.invitation_message
    )
}