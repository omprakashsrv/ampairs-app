package com.ampairs.workspace.store

import com.ampairs.common.model.PageResult
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.MemberDetailsResponse
import com.ampairs.workspace.api.model.MemberListResponse
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.db.entity.WorkspaceMemberEntity
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

/**
 * Store5 Factory for WorkspaceMember data management with pagination support
 * Following Store5 guidelines for proper offline-first architecture
 */
typealias WorkspaceMemberStore = Store<WorkspaceMemberKey, PageResult<WorkspaceMember>>

/**
 * Pagination-aware WorkspaceMemberKey for Store5
 * Each page is cached separately for optimal performance
 */
data class WorkspaceMemberKey(
    val userId: String, // Current logged-in user ID
    val workspaceId: String,
    val memberId: String? = null, // null for paginated list, non-null for single member
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "joinedAt",
    val sortDir: String = "desc"
) {
    // Helper functions to create keys
    companion object {
        fun forMember(userId: String, workspaceId: String, memberId: String) = WorkspaceMemberKey(
            userId = userId,
            workspaceId = workspaceId,
            memberId = memberId
        )

        fun forPage(userId: String, workspaceId: String, page: Int, size: Int = 20) =
            WorkspaceMemberKey(
                userId = userId,
                workspaceId = workspaceId,
                page = page,
                size = size
            )
    }
}

class WorkspaceMemberStoreFactory(
    private val memberApi: WorkspaceMemberApi,
    private val memberDao: WorkspaceMemberDao,
) {

    fun create(): WorkspaceMemberStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<WorkspaceMemberKey, PageResult<WorkspaceMember>> {
        return Fetcher.of { key ->
            val result: PageResult<WorkspaceMember> = if (key.memberId != null) {
                // Fetch single member details
                val response = memberApi.getMemberDetails(key.workspaceId, key.memberId)
                if (response.data != null && response.error == null) {
                    val member = response.data!!.toDomainModel().copy(
                        workspaceId = key.workspaceId,
                        permissions = response.data!!.permissions // Ensure permissions are copied
                    )
                    // Return single member as PageResult
                    PageResult(
                        content = listOf(member),
                        totalElements = 1,
                        totalPages = 1,
                        currentPage = 0,
                        pageSize = 1,
                        isFirst = true,
                        isLast = true,
                        isEmpty = false
                    )
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch member details")
                }
            } else {
                // Fetch paginated list of workspace members
                val response = memberApi.getWorkspaceMembers(
                    workspaceId = key.workspaceId,
                    page = key.page,
                    size = key.size,
                    sortBy = key.sortBy,
                    sortDir = key.sortDir
                )
                if (response.data != null && response.error == null) {
                    val pagedResponse = response.data!!
                    val members = pagedResponse.content.map {
                        it.toDomainModel().copy(workspaceId = key.workspaceId)
                    }

                    // Convert API PagedMemberResponse to domain PageResult
                    PageResult(
                        content = members,
                        totalElements = pagedResponse.totalElements.toInt(),
                        totalPages = pagedResponse.totalPages,
                        currentPage = pagedResponse.pageNumber,
                        pageSize = pagedResponse.pageSize,
                        isFirst = pagedResponse.first,
                        isLast = pagedResponse.last,
                        isEmpty = pagedResponse.empty
                    )
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch workspace members")
                }
            }
            result
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<WorkspaceMemberKey, PageResult<WorkspaceMember>, PageResult<WorkspaceMember>> {
        return SourceOfTruth.of(
            reader = { key ->
                if (key.memberId != null) {
                    // Read single member
                    memberDao.getWorkspaceMemberForUserFlow(
                        key.userId,
                        key.workspaceId,
                        key.memberId
                    )
                        .map { entity ->
                            if (entity != null) {
                                val member = entity.toDomainModel()
                                PageResult(
                                    content = listOf(member),
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
                    // Read paginated workspace members
                    memberDao.getWorkspaceMembersPaged(
                        key.userId,
                        key.workspaceId,
                        key.size,
                        key.page * key.size
                    )
                        .map { entities ->
                            val members = entities.map { it.toDomainModel() }
                            val totalCount = memberDao.getWorkspaceMemberCountForUser(
                                key.userId,
                                key.workspaceId
                            )
                            val totalPages = (totalCount + key.size - 1) / key.size

                            PageResult(
                                content = members,
                                totalElements = totalCount,
                                totalPages = totalPages,
                                currentPage = key.page,
                                pageSize = key.size,
                                isFirst = key.page == 0,
                                isLast = key.page >= totalPages - 1,
                                isEmpty = members.isEmpty()
                            )
                        }
                }
            },
            writer = { key, pageResult ->
                // Convert Domain models to entities with sync metadata
                val currentTime = currentTimeMillis()
                val entities = pageResult.content.map { member ->
                    member.toEntityModel(key.userId, key.workspaceId).copy(
                        sync_state = "SYNCED",
                        last_synced_at = currentTime,
                        server_updated_at = currentTime,
                        local_updated_at = currentTime
                    )
                }

                if (key.memberId != null) {
                    // Single member - replace existing
                    memberDao.deleteWorkspaceMemberForUser(
                        key.userId,
                        key.workspaceId,
                        key.memberId
                    )
                    memberDao.insertWorkspaceMembers(entities)
                } else {
                    // Paginated data - only clear and insert if it's the first page
                    // For subsequent pages, just insert new data
                    if (key.page == 0) {
                        memberDao.deleteAllWorkspaceMembersForUser(key.userId, key.workspaceId)
                    }
                    memberDao.insertWorkspaceMembers(entities)
                }
            }
        )
    }
}

// Extension functions for data model conversions
private fun MemberListResponse.toDomainModel(): WorkspaceMember {
    return WorkspaceMember(
        id = this.id,
        userId = this.userId,
        workspaceId = "", // Will be set by the caller
        name = this.user?.getDisplayName() ?: "Unknown",
        email = this.user?.email,
        phone = this.user?.phone,
        role = this.role,
        status = if (this.isActive) "ACTIVE" else "INACTIVE",
        joinedAt = this.joinedAt,
        lastActivity = this.lastActivityAt,
        permissions = emptyList(), // Will be populated from other sources
        avatarUrl = this.user?.profilePictureUrl
    )
}

private fun MemberDetailsResponse.toDomainModel(): WorkspaceMember {
    return WorkspaceMember(
        id = this.id,
        userId = this.userId,
        workspaceId = "", // Will be set by the caller
        name = this.user?.getDisplayName() ?: "Unknown",
        email = this.user?.email,
        phone = this.user?.phone,
        role = this.role,
        status = if (this.isActive) "ACTIVE" else "INACTIVE",
        joinedAt = this.joinedAt,
        lastActivity = this.lastActivityAt,
        permissions = this.permissions, // Use the actual permissions from API
        avatarUrl = this.user?.profilePictureUrl
    )
}

private fun MemberListResponse.toEntityModel(
    userId: String,
    workspaceId: String
): WorkspaceMemberEntity {
    return WorkspaceMemberEntity(
        id = this.id,
        user_id = userId, // Current logged-in user ID
        member_user_id = this.userId, // The actual member's user ID
        workspace_id = workspaceId,
        name = this.user?.getDisplayName() ?: "Unknown",
        email = this.user?.email ?: "",
        phone = this.user?.phone,
        role = this.role,
        status = if (this.isActive) "ACTIVE" else "INACTIVE",
        joined_at = this.joinedAt,
        last_activity = this.lastActivityAt,
        permissions = Json.encodeToString(this.permissions), // Will be populated from other sources as JSON string
        avatar_url = this.user?.profilePictureUrl,
        // Default sync metadata
        sync_state = "SYNCED",
        last_synced_at = currentTimeMillis(),
        local_updated_at = currentTimeMillis(),
        server_updated_at = currentTimeMillis(),
        pending_changes = "",
        conflict_data = "",
        retry_count = 0
    )
}

private fun WorkspaceMemberEntity.toDomainModel(): WorkspaceMember {
    return WorkspaceMember(
        id = this.id,
        userId = this.member_user_id,
        workspaceId = this.workspace_id,
        name = this.name,
        email = this.email.ifEmpty { null },
        phone = this.phone,
        role = this.role,
        status = this.status,
        joinedAt = this.joined_at,
        lastActivity = this.last_activity,
        permissions = Json.decodeFromString(this.permissions), // Will parse from JSON string if needed
        avatarUrl = this.avatar_url
    )
}

private fun WorkspaceMember.toEntityModel(
    userId: String,
    workspaceId: String
): WorkspaceMemberEntity {
    return WorkspaceMemberEntity(
        id = this.id,
        user_id = userId, // Current logged-in user ID
        member_user_id = this.userId, // The actual member's user ID
        workspace_id = workspaceId,
        name = this.name,
        email = this.email ?: "",
        phone = this.phone,
        role = this.role,
        status = this.status,
        joined_at = this.joinedAt,
        last_activity = this.lastActivity,
        permissions = Json.encodeToString(this.permissions), // Will be populated from other sources as JSON string
        avatar_url = this.avatarUrl,
        // Default sync metadata
        sync_state = "SYNCED",
        last_synced_at = currentTimeMillis(),
        local_updated_at = currentTimeMillis(),
        server_updated_at = currentTimeMillis(),
        pending_changes = "",
        conflict_data = "",
        retry_count = 0
    )
}