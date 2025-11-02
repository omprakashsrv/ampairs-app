package com.ampairs.workspace.store

import com.ampairs.common.model.PageResult
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceInvitationApi
import com.ampairs.workspace.api.model.InvitationApiModel
import com.ampairs.workspace.api.model.InvitationListResponse
import com.ampairs.workspace.db.dao.WorkspaceInvitationDao
import com.ampairs.workspace.db.entity.WorkspaceInvitationEntity
import com.ampairs.workspace.domain.WorkspaceInvitation
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

/**
 * Store5 Factory for WorkspaceInvitation data management with pagination support
 * Following Store5 guidelines for proper offline-first architecture
 */
typealias WorkspaceInvitationStore = Store<WorkspaceInvitationKey, PageResult<WorkspaceInvitation>>

/**
 * Pagination-aware WorkspaceInvitationKey for Store5
 * Each page is cached separately for optimal performance
 */
data class WorkspaceInvitationKey(
    val userId: String,
    val workspaceId: String,
    val invitationId: String? = null, // null for paginated list, non-null for single invitation
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "createdAt",
    val sortDir: String = "desc",
    val status: String? = null, // Optional filter by status
    val role: String? = null    // Optional filter by role
) {
    // Helper functions to create specific keys
    companion object {
        fun forInvitation(userId: String, workspaceId: String, invitationId: String) =
            WorkspaceInvitationKey(
                userId = userId,
                workspaceId = workspaceId,
                invitationId = invitationId
            )

        fun forPage(userId: String, workspaceId: String, page: Int, size: Int = 20) =
            WorkspaceInvitationKey(
                userId = userId,
                workspaceId = workspaceId,
                page = page,
                size = size
            )

        fun forPageWithFilters(
            userId: String,
            workspaceId: String,
            page: Int,
            size: Int = 20,
            status: String? = null,
            role: String? = null
        ) = WorkspaceInvitationKey(
            userId = userId,
            workspaceId = workspaceId,
            page = page,
            size = size,
            status = status,
            role = role
        )
    }
}

class WorkspaceInvitationStoreFactory(
    private val invitationApi: WorkspaceInvitationApi,
    private val invitationDao: WorkspaceInvitationDao,
) {

    fun create(): WorkspaceInvitationStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<WorkspaceInvitationKey, PageResult<WorkspaceInvitation>> {
        return Fetcher.of { key ->
            println("🌐 Store5 Fetcher: Fetching invitations for workspace ${key.workspaceId} (page=${key.page})")
            val result: PageResult<WorkspaceInvitation> = if (key.invitationId != null) {
                // Fetch single invitation (if API supports it)
                // For now, we'll throw an exception as the current API doesn't support single invitation fetch
                throw UnsupportedOperationException("Single invitation fetch not supported by API")
            } else {
                // Fetch paginated workspace invitations
                println("🌐 Store5 Fetcher: Making API call to getWorkspaceInvitations")
                val response = invitationApi.getWorkspaceInvitations(
                    workspaceId = key.workspaceId,
                    page = key.page,
                    size = key.size,
                    sortBy = key.sortBy,
                    sortDir = key.sortDir
                )

                println("🌐 Store5 Fetcher: API response - data=${response.data != null}, error=${response.error}")
                if (response.data != null && response.error == null) {
                    val pagedResponse = response.data!!
                    val invitations =
                        pagedResponse.content.map { invitationListResponse: InvitationListResponse ->
                            invitationListResponse.toDomainModel()
                        }

                    // Apply client-side filters if specified
                    val filteredInvitations = invitations.filter { invitation ->
                        val matchesStatus = key.status == null || invitation.status == key.status
                        val matchesRole = key.role == null || invitation.invitedRole == key.role
                        matchesStatus && matchesRole
                    }

                    // Convert API PagedInvitationResponse to domain PageResult
                    PageResult(
                        content = filteredInvitations,
                        totalElements = pagedResponse.totalElements,
                        totalPages = pagedResponse.totalPages,
                        currentPage = pagedResponse.page,
                        pageSize = pagedResponse.size,
                        isFirst = pagedResponse.isFirst,
                        isLast = pagedResponse.isLast,
                        isEmpty = filteredInvitations.isEmpty()
                    )
                } else {
                    throw Exception(
                        response.error?.message ?: "Failed to fetch workspace invitations"
                    )
                }
            }
            result
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<WorkspaceInvitationKey, PageResult<WorkspaceInvitation>, PageResult<WorkspaceInvitation>> {
        return SourceOfTruth.of(
            reader = { key ->
                if (key.invitationId != null) {
                    // Read single invitation
                    invitationDao.getInvitationByIdFlow(key.invitationId, key.userId)
                        .map { entity ->
                            if (entity != null) {
                                val invitation = entity.toDomainModel()
                                PageResult(
                                    content = listOf(invitation),
                                    totalElements = 1,
                                    totalPages = 1,
                                    currentPage = 0,
                                    pageSize = 1,
                                    isFirst = true,
                                    isLast = true,
                                    isEmpty = false
                                )
                            } else {
                                PageResult(
                                    content = emptyList(),
                                    totalElements = 0,
                                    totalPages = 0,
                                    currentPage = 0,
                                    pageSize = key.size,
                                    isFirst = true,
                                    isLast = true,
                                    isEmpty = true
                                )
                            }
                        }
                } else {
                    // Read paginated workspace invitations with filters
                    val baseFlow = if (key.status != null && key.role != null) {
                        invitationDao.getInvitationsByStatusAndRole(
                            key.userId,
                            key.workspaceId,
                            key.status,
                            key.role
                        )
                    } else if (key.status != null) {
                        invitationDao.getInvitationsByStatus(
                            key.userId,
                            key.workspaceId,
                            key.status
                        )
                    } else if (key.role != null) {
                        invitationDao.getInvitationsByRole(key.userId, key.workspaceId, key.role)
                    } else {
                        invitationDao.getInvitationsForWorkspacePaged(
                            key.userId,
                            key.workspaceId,
                            key.size,
                            key.page * key.size
                        )
                    }

                    baseFlow.map { entities ->
                        val invitations = entities.map { entity: WorkspaceInvitationEntity ->
                            entity.toDomainModel()
                        }
                        val totalCount = invitationDao.getInvitationCountForWorkspace(
                            key.userId,
                            key.workspaceId
                        )
                        val totalPages = (totalCount + key.size - 1) / key.size

                        PageResult(
                            content = invitations,
                            totalElements = totalCount,
                            totalPages = totalPages,
                            currentPage = key.page,
                            pageSize = key.size,
                            isFirst = key.page == 0,
                            isLast = key.page >= totalPages - 1,
                            isEmpty = invitations.isEmpty()
                        )
                    }
                }
            },
            writer = { key, pageResult ->
                // Convert Domain models to entities with sync metadata
                val currentTime = currentTimeMillis()
                val entities = pageResult.content.map { invitation: WorkspaceInvitation ->
                    invitation.toEntityModel(key.userId).copy(
                        sync_state = "SYNCED",
                        last_synced_at = currentTime,
                        server_updated_at = currentTime,
                        local_updated_at = currentTime
                    )
                }

                if (key.invitationId != null) {
                    // Single invitation - replace existing
                    invitationDao.deleteInvitation(key.invitationId, key.userId)
                    if (entities.isNotEmpty()) {
                        invitationDao.insertInvitations(entities)
                    }
                } else {
                    // Paginated data - only clear and insert if it's the first page
                    // For subsequent pages, just insert new data
                    if (key.page == 0) {
                        invitationDao.deleteAllInvitationsForWorkspace(key.workspaceId, key.userId)
                    }
                    if (entities.isNotEmpty()) {
                        invitationDao.insertInvitations(entities)
                    }
                }
            }
        )
    }
}

// Extension functions for data model conversions
private fun InvitationListResponse.toDomainModel(): WorkspaceInvitation {
    return WorkspaceInvitation(
        id = this.id,
        workspaceId = this.workspaceId, // Backend list response doesn't include workspace_id
        countryCode = this.countryCode ?: 0,
        phone = this.phone ?: "",
        recipientName = this.email, // Use email as name fallback
        invitedRole = this.role,
        status = this.status,
        createdAt = this.createdAt,
        expiresAt = this.expiresAt,
        sentByName = this.inviterName ?: "Unknown",
        sentByEmail = this.invitedBy ?: "Unknown",
        emailSent = true, // Backend doesn't provide detailed delivery status
        emailDelivered = true,
        emailOpened = false,
        linkClicked = false,
        resendCount = this.sendCount,
        invitationMessage = this.message
    )
}

private fun InvitationApiModel.toDomainModel(): WorkspaceInvitation {
    return WorkspaceInvitation(
        id = this.id,
        workspaceId = this.workspaceId,
        countryCode = this.countryCode ?: 0,
        phone = this.phone ?: "",
        recipientName = this.email, // Use email as name fallback
        invitedRole = this.role,
        status = this.status,
        createdAt = this.createdAt,
        expiresAt = this.expiresAt,
        sentByName = this.inviterName ?: "Unknown",
        sentByEmail = this.invitedBy ?: "Unknown",
        emailSent = true, // Backend doesn't provide detailed delivery status
        emailDelivered = true,
        emailOpened = false,
        linkClicked = false,
        resendCount = this.sendCount,
        invitationMessage = this.message
    )
}

private fun WorkspaceInvitationEntity.toDomainModel(): WorkspaceInvitation {
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

private fun WorkspaceInvitation.toEntityModel(userId: String): WorkspaceInvitationEntity {
    return WorkspaceInvitationEntity(
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