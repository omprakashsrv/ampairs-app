package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Offline-first workspace member repository - simplified version for compilation
 * TODO: Implement full Store5 pattern with conflict resolution
 */
class OfflineFirstWorkspaceMemberRepository(
    private val memberApi: WorkspaceMemberApi,
    private val memberDao: WorkspaceMemberDao,
    private val tokenRepository: TokenRepository
    // TODO: Add workspace member store and sync manager
) {

    private suspend fun getCurrentUserId(): String? {
        return tokenRepository.getCurrentUserId()
    }

    /**
     * Get workspace member by ID with offline-first approach
     * TODO: Implement Store5 pattern
     */
    suspend fun getWorkspaceMemberById(workspaceId: String, memberId: String): WorkspaceMember? {
        val currentUserId = getCurrentUserId() ?: return null
        return memberDao.getWorkspaceMemberForUser(currentUserId, workspaceId, memberId)?.asDomainModel()
    }

    /**
     * Update workspace member - simplified implementation
     * TODO: Implement optimistic updates and conflict resolution
     */
    suspend fun updateWorkspaceMember(
        workspaceId: String,
        memberId: String,
        request: UpdateMemberRequest
    ): WorkspaceMember {
        val response = memberApi.updateMember(workspaceId, memberId, request)

        return if (response.error == null && response.data != null) {
            val memberData = response.data!!
            val member = WorkspaceMember(
                id = memberData.id,
                userId = memberData.userId,
                workspaceId = memberData.workspaceId,
                email = memberData.email,
                phone = memberData.phone,
                name = memberData.name,
                role = memberData.role,
                status = memberData.status,
                joinedAt = memberData.joinedAt,
                lastActivity = memberData.lastActivityAt ?: "",
                permissions = memberData.permissions,
                avatarUrl = memberData.avatarUrl,
            )

            // Update in local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            val currentTime = currentTimeMillis()
            
            val memberEntity = member.asDatabaseModel().copy(
                user_id = currentUserId,
                sync_state = "SYNCED",
                last_synced_at = currentTime,
                server_updated_at = currentTime,
                local_updated_at = currentTime
            )
            memberDao.insertWorkspaceMember(memberEntity)

            member
        } else {
            throw Exception(response.error?.message ?: "Failed to update workspace member")
        }
    }

    /**
     * Get workspace members from API with pagination info
     * Falls back to local data when offline
     */
    suspend fun getWorkspaceMembers(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
        forceRefresh: Boolean = false
    ): PageResult<WorkspaceMember> {
        return try {
            if (!forceRefresh) {
                // Try to get from local first
                val localMembers = getLocalWorkspaceMembers(workspaceId).first()
                if (localMembers.isNotEmpty()) {
                    // Return paginated local results
                    val startIndex = page * size
                    val endIndex = minOf(startIndex + size, localMembers.size)
                    val pageContent = if (startIndex < localMembers.size) {
                        localMembers.subList(startIndex, endIndex)
                    } else {
                        emptyList()
                    }
                    
                    return PageResult(
                        content = pageContent,
                        totalElements = localMembers.size,
                        totalPages = (localMembers.size + size - 1) / size,
                        currentPage = page,
                        pageSize = size,
                        isFirst = page == 0,
                        isLast = endIndex >= localMembers.size,
                        isEmpty = localMembers.isEmpty()
                    )
                }
            }
            
            // Fetch from network
            val response = memberApi.getWorkspaceMembers(workspaceId, page, size, sortBy, sortDir)
            
            if (response.error == null && response.data != null) {
                val pagedResponse = response.data!!
                
                val members = pagedResponse.content.map { memberListItem ->
                    WorkspaceMember(
                        id = memberListItem.id,
                        userId = memberListItem.userId,
                        workspaceId = workspaceId,
                        email = memberListItem.user?.email ?: "",
                        name = memberListItem.user?.getDisplayName() ?: memberListItem.userId,
                        role = memberListItem.role,
                        status = if (memberListItem.isActive) "ACTIVE" else "INACTIVE",
                        joinedAt = memberListItem.joinedAt,
                        lastActivity = memberListItem.lastActivityAt,
                        permissions = emptyList(),
                        avatarUrl = memberListItem.user?.profilePictureUrl,
                        phone = memberListItem.user?.phone,
                    )
                }

                // Save to local database with current user association
                val currentUserId = getCurrentUserId() ?: "unknown_user"
                val currentTime = currentTimeMillis()
                
                members.forEach { member ->
                    val memberEntity = member.asDatabaseModel().copy(
                        user_id = currentUserId,
                        sync_state = "SYNCED",
                        last_synced_at = currentTime,
                        server_updated_at = currentTime,
                        local_updated_at = currentTime
                    )
                    memberDao.insertWorkspaceMember(memberEntity)
                }

                PageResult(
                    content = members,
                    totalElements = pagedResponse.totalElements.toInt(),
                    totalPages = pagedResponse.totalPages,
                    currentPage = pagedResponse.pageNumber,
                    pageSize = pagedResponse.pageSize,
                    isFirst = pagedResponse.first,
                    isLast = pagedResponse.last,
                    isEmpty = members.isEmpty()
                )
            } else {
                // Network failed, return local data
                val localMembers = getLocalWorkspaceMembers(workspaceId).first()
                
                val startIndex = page * size
                val endIndex = minOf(startIndex + size, localMembers.size)
                val pageContent = if (startIndex < localMembers.size) {
                    localMembers.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
                
                PageResult(
                    content = pageContent,
                    totalElements = localMembers.size,
                    totalPages = (localMembers.size + size - 1) / size,
                    currentPage = page,
                    pageSize = size,
                    isFirst = page == 0,
                    isLast = endIndex >= localMembers.size,
                    isEmpty = localMembers.isEmpty()
                )
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch workspace members: ${e.message}")
        }
    }

    /**
     * Remove member from workspace
     */
    suspend fun removeMember(workspaceId: String, memberId: String): String {
        val response = memberApi.removeMember(workspaceId, memberId)

        return if (response.error == null && response.data != null) {
            // Remove from local database
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                memberDao.deleteWorkspaceMemberForUser(currentUserId, workspaceId, memberId)
            }
            response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to remove member")
        }
    }

    /**
     * Get workspace members from local database for current user
     */
    suspend fun getLocalWorkspaceMembers(workspaceId: String): Flow<List<WorkspaceMember>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        return memberDao.getWorkspaceMembersForUser(currentUserId, workspaceId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Get workspace member from local database
     */
    suspend fun getLocalWorkspaceMember(workspaceId: String, memberId: String): WorkspaceMember? {
        val currentUserId = getCurrentUserId() ?: return null
        return memberDao.getWorkspaceMemberForUser(currentUserId, workspaceId, memberId)?.asDomainModel()
    }

    /**
     * Search workspace members locally for current user
     */
    suspend fun searchWorkspaceMembersLocally(workspaceId: String, query: String): Flow<List<WorkspaceMember>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        return memberDao.searchWorkspaceMembersForUser(currentUserId, workspaceId, query).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Clear local workspace members for current user
     */
    suspend fun clearLocalWorkspaceMembers(workspaceId: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            memberDao.deleteAllWorkspaceMembersForUser(currentUserId, workspaceId)
        }
    }
}

/**
 * Result of workspace member update operation
 */
sealed class WorkspaceMemberUpdateResult {
    data class Success(val message: String) : WorkspaceMemberUpdateResult()
    data class Failed(val message: String) : WorkspaceMemberUpdateResult()
    data class ConflictDetected(val message: String) : WorkspaceMemberUpdateResult()
}