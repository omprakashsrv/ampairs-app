package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * Repository for workspace member management
 *
 * Handles member-related operations including viewing, updating, and removing
 * members with proper data transformation and error handling.
 */
class WorkspaceMemberRepository(
    private val memberApi: WorkspaceMemberApi,
    private val memberDao: WorkspaceMemberDao,
    private val tokenRepository: TokenRepository,
) {

    private suspend fun getCurrentUserId(): String? {
        return tokenRepository.getCurrentUserId()
    }

    /**
     * Get current user's role and permissions in workspace
     */
    suspend fun getMyRole(workspaceId: String): UserRoleResponse {
        val response = memberApi.getMyRole(workspaceId)
        if (response.error == null && response.data != null) {
            return response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to get user role")
        }
    }

}

/**
 * Domain model for user role and permissions
 */
data class UserRole(
    val userId: String,
    val workspaceId: String,
    val currentRole: String,
    val membershipStatus: String,
    val joinedAt: String,
    val lastActivity: String? = null,
    val roleHierarchy: Map<String, Boolean>,
    val permissions: Map<String, Map<String, Boolean>>,
    val moduleAccess: List<String>,
    val restrictions: Map<String, Any>? = null,
)
