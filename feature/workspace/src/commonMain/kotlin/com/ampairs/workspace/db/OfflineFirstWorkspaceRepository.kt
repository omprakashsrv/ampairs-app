package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.di.AppScope
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.domain.Workspace
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first workspace repository using Store5 pattern
 * Provides consistent offline-first data access with automatic sync
 */
@Inject
@SingleIn(AppScope::class)
class OfflineFirstWorkspaceRepository(
    private val workspaceApi: WorkspaceApi,
    private val workspaceDao: WorkspaceDao,
    private val tokenRepository: TokenRepository
) {

    private suspend fun getCurrentUserId(): String? {
        return tokenRepository.getCurrentUserId()
    }

    /**
     * Get workspace by ID with offline-first approach
     * TODO: Implement Store5 pattern
     */
    suspend fun getWorkspaceById(workspaceId: String): Workspace? {
        val currentUserId = getCurrentUserId()
        return if (currentUserId != null) {
            workspaceDao.getWorkspaceByIdForUser(workspaceId, currentUserId)?.asDomainModel()
        } else {
            workspaceDao.getWorkspaceById(workspaceId)?.asDomainModel()
        }
    }

    /**
     * Update workspace - simplified implementation
     * TODO: Implement optimistic updates and conflict resolution
     */
    suspend fun updateWorkspace(
        workspaceId: String, 
        request: UpdateWorkspaceRequest
    ): Workspace {
        val response = workspaceApi.updateWorkspace(workspaceId, request)

        return if (response.error == null && response.data != null) {
            val workspaceData = response.data!!
            val workspace = workspaceData.asDomainModel()

            // Update in local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            val currentTime = currentTimeMillis()
            
            val workspaceEntity = workspace.asDatabaseModel().copy(
                user_id = currentUserId,
                sync_state = "SYNCED",
                last_synced_at = currentTime,
                server_updated_at = currentTime,
                local_updated_at = currentTime
            )
            workspaceDao.insertWorkspace(workspaceEntity)

            workspace
        } else {
            throw Exception(response.error?.message ?: "Failed to update workspace")
        }
    }

    fun observeWorkspaces(): Flow<List<Workspace>> = flow {
        val userId = getCurrentUserId()
        val source = if (userId != null) {
            workspaceDao.getAllWorkspacesForUser(userId)
        } else {
            workspaceDao.getAllWorkspaces()
        }
        emitAll(source.map { entities -> entities.map { it.asDomainModel() } })
    }

    suspend fun syncWorkspaces() {
        val currentUserId = getCurrentUserId() ?: return
        try {
            val response = workspaceApi.getUserWorkspaces(page = 0, size = 100)
            if (response.data != null && response.error == null) {
                val currentTime = currentTimeMillis()
                for (apiModel in response.data!!.content) {
                    val entity = com.ampairs.workspace.db.entity.WorkspaceEntity(
                        id = apiModel.id,
                        name = apiModel.name,
                        slug = apiModel.slug,
                        description = apiModel.description ?: "",
                        workspaceType = apiModel.workspaceType,
                        avatarUrl = apiModel.avatarUrl ?: "",
                        isActive = true,
                        subscriptionPlan = apiModel.subscriptionPlan,
                        memberCount = apiModel.memberCount,
                        lastActivityAt = apiModel.lastActivityAt ?: "",
                        createdAt = apiModel.createdAt,
                        createdBy = currentUserId,
                        updatedAt = apiModel.lastActivityAt ?: apiModel.createdAt,
                        user_id = currentUserId,
                        sync_state = "SYNCED",
                        last_synced_at = currentTime,
                        server_updated_at = currentTime
                    )
                    workspaceDao.insertWorkspace(entity)
                }
            }
        } catch (_: Exception) {
            // Graceful failure — UI continues with cached data
        }
    }

    suspend fun searchWorkspaces(
        query: String,
        page: Int = 0,
        size: Int = 10
    ): Flow<List<Workspace>> {
        return searchWorkspacesLocally(query)
    }

    /**
     * Create a new workspace
     */
    suspend fun createWorkspace(request: CreateWorkspaceRequest): Workspace {
        // logger.d { "Creating workspace: ${request.name}" }
        
        val response = workspaceApi.createWorkspace(request)

        return if (response.error == null && response.data != null) {
            val workspaceData = response.data!!
            val workspace = workspaceData.asDomainModel()

            // Save to local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            val currentTime = currentTimeMillis()
            
            val workspaceEntity = workspace.asDatabaseModel().copy(
                user_id = currentUserId,
                sync_state = "SYNCED",
                last_synced_at = currentTime,
                server_updated_at = currentTime,
                local_updated_at = currentTime
            )
            workspaceDao.insertWorkspace(workspaceEntity)

            workspace
        } else {
            throw Exception(response.error?.message ?: "Failed to create workspace")
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
        val currentUserId = getCurrentUserId() ?: return workspaceDao.getAllWorkspaces().map { entities ->
            entities.map { it.asDomainModel() }
        }

        return workspaceDao.getAllWorkspacesForUser(currentUserId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Search workspaces locally for current user
     */
    suspend fun searchWorkspacesLocally(query: String): Flow<List<Workspace>> {
        val currentUserId = getCurrentUserId() ?: return workspaceDao.searchWorkspaces(query).map { entities ->
            entities.map { it.asDomainModel() }
        }

        return workspaceDao.searchWorkspacesForUser(currentUserId, query).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Clear local workspaces for current user
     */
    suspend fun clearLocalWorkspaces() {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            workspaceDao.deleteAllWorkspacesForUser(currentUserId)
        } else {
            workspaceDao.deleteAllWorkspaces()
        }
    }
}

/**
 * Result of workspace update operation
 */
sealed class WorkspaceUpdateResult {
    data class Success(val message: String) : WorkspaceUpdateResult()
    data class Failed(val message: String) : WorkspaceUpdateResult()
    data class ConflictDetected(val message: String) : WorkspaceUpdateResult()
}