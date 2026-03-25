package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.domain.Workspace
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import com.ampairs.workspace.store.WorkspaceKey
import com.ampairs.workspace.store.WorkspaceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Offline-first workspace repository using Store5 pattern
 * Provides consistent offline-first data access with automatic sync
 */
class OfflineFirstWorkspaceRepository(
    private val workspaceApi: WorkspaceApi,
    private val workspaceDao: WorkspaceDao,
    private val tokenRepository: TokenRepository,
    private val workspaceStore: WorkspaceStore
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

    /**
     * Get user workspaces using Store5 pattern
     * Provides offline-first access with automatic caching and sync
     */
    suspend fun getUserWorkspaces(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
        forceRefresh: Boolean = false
    ): Flow<StoreReadResponse<PageResult<Workspace>>> {
        val currentUserId = tokenRepository.getCurrentUserId() 
            ?: throw IllegalStateException("User not authenticated")
        
        val key = WorkspaceKey(
            userId = currentUserId,
            page = page,
            size = size,
            sortBy = sortBy,
            sortDir = sortDir
        )
        
        val request = if (forceRefresh) {
            StoreReadRequest.fresh(key)
        } else {
            StoreReadRequest.cached(key, refresh = true)
        }
        
        return workspaceStore.stream(request)
    }
    
    /**
     * Get cached workspaces only (for offline mode)
     */
    suspend fun getCachedWorkspaces(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDir: String = "desc"
    ): Flow<StoreReadResponse<PageResult<Workspace>>> {
        val currentUserId = tokenRepository.getCurrentUserId() 
            ?: throw IllegalStateException("User not authenticated")
        
        val key = WorkspaceKey(
            userId = currentUserId,
            page = page,
            size = size,
            sortBy = sortBy,
            sortDir = sortDir
        )
        
        return workspaceStore.stream(StoreReadRequest.cached(key, refresh = false))
    }
    
    /**
     * Search workspaces locally for current user
     */
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