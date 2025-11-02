package com.ampairs.workspace.db

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.domain.Workspace
import com.ampairs.common.model.PageResult
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import com.ampairs.auth.api.TokenRepository
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkspaceRepository(
    private val workspaceApi: WorkspaceApi,
    private val workspaceDao: WorkspaceDao,
    private val tokenRepository: TokenRepository, // Add dependency to get current user
) {

    private suspend fun getCurrentUserId(): String? {
        return tokenRepository.getCurrentUserId()
    }

    /**
     * Create a new workspace
     */
    suspend fun createWorkspace(request: CreateWorkspaceRequest): Workspace {
        val response = workspaceApi.createWorkspace(request)

        return if (response.error == null && response.data != null) {
            val workspaceData = response.data!!
            val workspace = workspaceData.asDomainModel()

            // Save to local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            workspaceDao.insertWorkspace(workspace.asDatabaseModel().copy(user_id = currentUserId))

            workspace
        } else {
            throw Exception(response.error?.message ?: "Failed to create workspace")
        }
    }

    /**
     * Update an existing workspace
     */
    suspend fun updateWorkspace(workspaceId: String, request: UpdateWorkspaceRequest): Workspace {
        val response = workspaceApi.updateWorkspace(workspaceId, request)

        return if (response.error == null && response.data != null) {
            val workspaceData = response.data!!
            val workspace = workspaceData.asDomainModel()

            // Update in local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            workspaceDao.insertWorkspace(workspace.asDatabaseModel().copy(user_id = currentUserId))

            workspace
        } else {
            throw Exception(response.error?.message ?: "Failed to update workspace")
        }
    }

    /**
     * Check slug availability
     */
    suspend fun checkSlugAvailability(slug: String): Map<String, Boolean> {
        val response = workspaceApi.checkSlugAvailability(slug)

        return if (response.error == null && response.data != null) {
            response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to check slug availability")
        }
    }

    /**
     * Get workspaces from local database for current user
     */
    suspend fun getLocalWorkspaces(): Flow<List<Workspace>> {
        val currentUserId =
            getCurrentUserId() ?: return workspaceDao.getAllWorkspaces().map { entities ->
                entities.map { it.asDomainModel() }
            }

        return workspaceDao.getAllWorkspacesForUser(currentUserId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Get workspace by ID for current user
     */
    suspend fun getWorkspaceById(workspaceId: String): Workspace? {
        val currentUserId = getCurrentUserId()
        return if (currentUserId != null) {
            workspaceDao.getWorkspaceByIdForUser(workspaceId, currentUserId)?.asDomainModel()
        } else {
            workspaceDao.getWorkspaceById(workspaceId)?.asDomainModel()
        }
    }

}