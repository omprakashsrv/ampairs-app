package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.model.PageResult
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceInvitationApi
import com.ampairs.workspace.api.model.CreateInvitationRequest
import com.ampairs.workspace.api.model.InvitationListResponse
import com.ampairs.workspace.api.model.ResendInvitationRequest
import com.ampairs.workspace.db.dao.WorkspaceInvitationDao
import com.ampairs.workspace.db.entity.WorkspaceInvitationEntity
import com.ampairs.workspace.domain.InvitationAcceptanceResult
import com.ampairs.workspace.domain.WorkspaceInvitation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
class OfflineFirstWorkspaceInvitationRepository(
    private val invitationApi: WorkspaceInvitationApi,
    private val invitationDao: WorkspaceInvitationDao,
    private val tokenRepository: TokenRepository
) {

    private suspend fun getCurrentUserId(): String =
        tokenRepository.getCurrentUserId() ?: throw IllegalStateException("User not authenticated")

    fun getWorkspaceInvitations(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
        refresh: Boolean = false
    ): Flow<PageResult<WorkspaceInvitation>> = flow {
        val userId = getCurrentUserId()

        if (!refresh) {
            val cached = invitationDao.getInvitationsForWorkspacePaged(userId, workspaceId, size, page * size).first()
            if (cached.isNotEmpty()) emit(buildPageResultFromEntities(cached, page, size))
        }

        val response = invitationApi.getWorkspaceInvitations(workspaceId, page, size, sortBy, sortDir)
        if (response.data != null && response.error == null) {
            val paged = response.data!!
            val currentTime = currentTimeMillis()
            val entities = paged.content.map { it.toEntity(userId, workspaceId, currentTime) }
            if (page == 0) invitationDao.deleteAllInvitationsForWorkspace(workspaceId, userId)
            invitationDao.insertInvitations(entities)

            emit(PageResult(
                content = entities.map { it.toDomainModel() },
                totalElements = paged.totalElements,
                totalPages = paged.totalPages,
                currentPage = paged.page,
                pageSize = paged.size,
                isFirst = paged.isFirst,
                isLast = paged.isLast,
                isEmpty = entities.isEmpty()
            ))
        } else if (response.error != null) {
            throw Exception(response.error!!.message)
        }
    }

    fun getFilteredInvitations(
        workspaceId: String,
        status: String? = null,
        role: String? = null,
        page: Int = 0,
        size: Int = 20,
        @Suppress("UNUSED_PARAMETER") refresh: Boolean = false
    ): Flow<PageResult<WorkspaceInvitation>> = flow {
        val userId = getCurrentUserId()

        val cached: List<WorkspaceInvitationEntity> = when {
            status != null && role != null ->
                invitationDao.getInvitationsByStatusAndRole(userId, workspaceId, status, role).first()
            status != null ->
                invitationDao.getInvitationsByStatus(userId, workspaceId, status).first()
            role != null ->
                invitationDao.getInvitationsByRole(userId, workspaceId, role).first()
            else ->
                invitationDao.getInvitationsForWorkspace(userId, workspaceId).first()
        }

        if (cached.isNotEmpty()) emit(buildPageResultFromEntities(cached, page, size))

        val response = invitationApi.getWorkspaceInvitations(workspaceId, page, size)
        if (response.data != null && response.error == null) {
            val paged = response.data!!
            val currentTime = currentTimeMillis()
            val entities = paged.content.map { it.toEntity(userId, workspaceId, currentTime) }
            if (page == 0) invitationDao.deleteAllInvitationsForWorkspace(workspaceId, userId)
            invitationDao.insertInvitations(entities)

            val filtered = entities.filter { entity ->
                (status == null || entity.status == status) &&
                        (role == null || entity.invited_role == role)
            }
            emit(buildPageResultFromEntities(filtered, page, size))
        }
    }

    suspend fun createInvitation(workspaceId: String, request: CreateInvitationRequest): WorkspaceInvitation {
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
                emailSent = true,
                emailDelivered = true,
                emailOpened = false,
                linkClicked = false,
                resendCount = invitationData.sendCount,
                invitationMessage = invitationData.message
            )
            val userId = getCurrentUserId()
            val currentTime = currentTimeMillis()
            val entity = invitation.toEntityModel(userId).copy(
                sync_state = "SYNCED",
                last_synced_at = currentTime,
                server_updated_at = currentTime,
                local_updated_at = currentTime
            )
            invitationDao.insertInvitation(entity)
            return invitation
        } else {
            throw Exception(response.error?.message ?: "Failed to create invitation")
        }
    }

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

    suspend fun resendInvitation(
        workspaceId: String,
        invitationId: String,
        request: ResendInvitationRequest = ResendInvitationRequest(),
    ): WorkspaceInvitation {
        val currentUserId = getCurrentUserId()
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
                    emailSent = true,
                    emailDelivered = true,
                    emailOpened = false,
                    linkClicked = false,
                    resendCount = invitationData.sendCount,
                    invitationMessage = invitationData.message
                )
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
                throw Exception(response.error?.message ?: "Failed to resend invitation")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun cancelInvitation(workspaceId: String, invitationId: String): String {
        val currentUserId = getCurrentUserId()
        invitationDao.updateInvitationStatus(invitationId, currentUserId, "CANCELLED")
        try {
            val response = invitationApi.cancelInvitation(workspaceId, invitationId)
            if (response.error == null && response.data != null) {
                val currentTime = currentTimeMillis()
                invitationDao.updateSyncState(invitationId, currentUserId, "SYNCED", currentTime)
                return response.data!!
            } else {
                throw Exception(response.error?.message ?: "Failed to cancel invitation")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun searchInvitations(workspaceId: String, query: String): Flow<List<WorkspaceInvitation>> = flow {
        val userId = getCurrentUserId()
        invitationDao.searchInvitations(userId, workspaceId, query)
            .map { entities -> entities.map { it.toDomainModel() } }
            .collect { emit(it) }
    }

    suspend fun clearCache(workspaceId: String) {
        val currentUserId = getCurrentUserId()
        invitationDao.deleteAllInvitationsForWorkspace(workspaceId, currentUserId)
    }

    private fun buildPageResultFromEntities(
        entities: List<WorkspaceInvitationEntity>,
        page: Int,
        size: Int
    ): PageResult<WorkspaceInvitation> {
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, entities.size)
        val pageContent = if (startIndex < entities.size) entities.subList(startIndex, endIndex) else emptyList()
        return PageResult(
            content = pageContent.map { it.toDomainModel() },
            totalElements = entities.size,
            totalPages = (entities.size + size - 1) / size,
            currentPage = page,
            pageSize = size,
            isFirst = page == 0,
            isLast = endIndex >= entities.size,
            isEmpty = pageContent.isEmpty()
        )
    }
}

private fun InvitationListResponse.toEntity(
    userId: String,
    workspaceId: String,
    currentTime: Long
): WorkspaceInvitationEntity = WorkspaceInvitationEntity(
    id = id,
    user_id = userId,
    workspace_id = workspaceId,
    country_code = countryCode ?: 0,
    phone = phone ?: "",
    recipient_name = email,
    invited_role = role,
    status = status,
    created_at = createdAt,
    expires_at = expiresAt,
    sent_by_name = inviterName ?: "",
    sent_by_email = invitedBy ?: "",
    email_sent = false,
    email_delivered = false,
    email_opened = false,
    link_clicked = false,
    resend_count = sendCount,
    invitation_message = message,
    sync_state = "SYNCED",
    last_synced_at = currentTime,
    server_updated_at = currentTime,
    local_updated_at = currentTime
)

private fun WorkspaceInvitationEntity.toDomainModel(): WorkspaceInvitation = WorkspaceInvitation(
    id = id,
    workspaceId = workspace_id,
    countryCode = country_code,
    phone = phone,
    recipientName = recipient_name,
    invitedRole = invited_role,
    status = status,
    createdAt = created_at,
    expiresAt = expires_at,
    sentByName = sent_by_name,
    sentByEmail = sent_by_email,
    emailSent = email_sent,
    emailDelivered = email_delivered,
    emailOpened = email_opened,
    linkClicked = link_clicked,
    resendCount = resend_count,
    invitationMessage = invitation_message
)

private fun WorkspaceInvitation.toEntityModel(userId: String): WorkspaceInvitationEntity = WorkspaceInvitationEntity(
    id = id,
    user_id = userId,
    workspace_id = workspaceId,
    country_code = countryCode,
    phone = phone,
    recipient_name = recipientName,
    invited_role = invitedRole,
    status = status,
    created_at = createdAt,
    expires_at = expiresAt,
    sent_by_name = sentByName,
    sent_by_email = sentByEmail,
    email_sent = emailSent,
    email_delivered = emailDelivered,
    email_opened = emailOpened,
    link_clicked = linkClicked,
    resend_count = resendCount,
    invitation_message = invitationMessage,
    sync_state = "SYNCED",
    last_synced_at = currentTimeMillis(),
    local_updated_at = currentTimeMillis(),
    server_updated_at = currentTimeMillis()
)
