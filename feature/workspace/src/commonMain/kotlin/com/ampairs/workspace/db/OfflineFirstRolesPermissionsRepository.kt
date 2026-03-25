package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.api.model.WorkspacePermissionResponse
import com.ampairs.workspace.db.dao.WorkspaceRoleDao
import com.ampairs.workspace.db.dao.WorkspacePermissionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first repository for workspace roles and permissions
 * Implements Store5 pattern for caching and synchronization
 */
class OfflineFirstRolesPermissionsRepository(
    private val memberApi: WorkspaceMemberApi,
    private val roleDao: WorkspaceRoleDao,
    private val permissionDao: WorkspacePermissionDao,
    private val tokenRepository: TokenRepository,
) {

    private suspend fun getCurrentUserId(): String? {
        return tokenRepository.getCurrentUserId()
    }

    /**
     * Get workspace roles with offline-first approach
     * Returns cached data immediately, then fetches fresh data from network
     */
    fun getWorkspaceRoles(workspaceId: String): Flow<List<WorkspaceRole>> = flow {
        val currentUserId =
            getCurrentUserId() ?: throw IllegalStateException("User not authenticated")

        // First, emit cached data if available
        val cachedRoles = roleDao.getWorkspaceRolesForUser(currentUserId, workspaceId).firstOrNull()
        if (!cachedRoles.isNullOrEmpty()) {
            emit(cachedRoles.toApiModels())
        }

        // Fetch fresh data from network
        val networkResponse = memberApi.getAvailableRoles(workspaceId)

        if (networkResponse.error == null && networkResponse.data != null) {
            val roles = networkResponse.data!!

            // Save to cache
            val roleEntities = roles.toEntities(currentUserId, workspaceId)

            // Clear old cache and insert new data
            roleDao.deleteAllWorkspaceRolesForUser(currentUserId, workspaceId)
            roleDao.insertWorkspaceRoles(roleEntities)

            // Emit fresh data
            emit(roles)
        } else {
            // If no cached data, throw; otherwise we've already emitted cached data above
            val fallbackRoles = roleDao.getWorkspaceRolesForUser(currentUserId, workspaceId).firstOrNull()
            if (fallbackRoles.isNullOrEmpty()) {
                throw Exception(networkResponse.error?.message ?: "Failed to fetch roles")
            } else {
                emit(fallbackRoles.toApiModels())
            }
        }
    }

    /**
     * Get workspace permissions with offline-first approach
     * Returns cached data immediately, then fetches fresh data from network
     */
    fun getWorkspacePermissions(workspaceId: String): Flow<Map<String, Map<String, Boolean>>> =
        flow {
            val currentUserId =
                getCurrentUserId() ?: throw IllegalStateException("User not authenticated")

            // First, emit cached data if available
            val cachedPermissions =
                permissionDao.getWorkspacePermissionsListForUser(currentUserId, workspaceId)
            if (cachedPermissions.isNotEmpty()) {
                emit(cachedPermissions.toPermissionMap())
            }

            // Fetch fresh data from network
            val networkResponse = memberApi.getAvailablePermissions(workspaceId)

            if (networkResponse.error == null && networkResponse.data != null) {
                val permissions = networkResponse.data!!

                // Save to cache
                val permissionEntities =
                    permissions.toPermissionEntities(currentUserId, workspaceId)

                // Clear old cache and insert new data
                permissionDao.deleteAllWorkspacePermissionsForUser(currentUserId, workspaceId)
                permissionDao.insertWorkspacePermissions(permissionEntities)

                // Convert permissions list to map format for UI
                val permissionsMap = convertPermissionsListToMap(permissions)
                
                // Emit fresh data
                emit(permissionsMap)
            } else {
                // If no cached data, throw; otherwise we've already emitted cached data above
                val fallbackPermissions =
                    permissionDao.getWorkspacePermissionsListForUser(currentUserId, workspaceId)
                if (fallbackPermissions.isEmpty()) {
                    throw Exception(networkResponse.error?.message ?: "Failed to fetch permissions")
                } else {
                    emit(fallbackPermissions.toPermissionMap())
                }
            }
        }

    /**
     * Get cached roles only (for immediate UI updates)
     */
    suspend fun getCachedRoles(workspaceId: String): Flow<List<WorkspaceRole>> {
        val currentUserId = getCurrentUserId() ?: return flow { emit(emptyList()) }

        return roleDao.getWorkspaceRolesForUser(currentUserId, workspaceId).map { entities ->
            entities.toApiModels()
        }
    }

    /**
     * Get cached permissions only (for immediate UI updates)
     */
    suspend fun getCachedPermissions(workspaceId: String): Map<String, Map<String, Boolean>> {
        val currentUserId = getCurrentUserId() ?: return emptyMap()

        val permissionEntities =
            permissionDao.getWorkspacePermissionsListForUser(currentUserId, workspaceId)
        return permissionEntities.toPermissionMap()
    }

    /**
     * Force refresh roles and permissions from network
     */
    suspend fun refreshRolesAndPermissions(workspaceId: String): Pair<List<WorkspaceRole>, Map<String, Map<String, Boolean>>> {
        val currentUserId =
            getCurrentUserId() ?: throw Exception("User not authenticated")

        val rolesResponse = memberApi.getAvailableRoles(workspaceId)
        val permissionsResponse = memberApi.getAvailablePermissions(workspaceId)

        if (rolesResponse.error == null && rolesResponse.data != null && permissionsResponse.error == null && permissionsResponse.data != null) {
            val roles = rolesResponse.data!!
            val permissionsList = permissionsResponse.data!!
            
            // Convert permissions list to map format for UI
            val permissionsMap = convertPermissionsListToMap(permissionsList)

            // Update cache
            roleDao.deleteAllWorkspaceRolesForUser(currentUserId, workspaceId)
            roleDao.insertWorkspaceRoles(roles.toEntities(currentUserId, workspaceId))

            permissionDao.deleteAllWorkspacePermissionsForUser(currentUserId, workspaceId)
            permissionDao.insertWorkspacePermissions(
                permissionsList.toPermissionEntities(
                    currentUserId,
                    workspaceId
                )
            )

            return Pair(roles, permissionsMap)
        } else {
            throw Exception(rolesResponse.error?.message ?: permissionsResponse.error?.message ?: "Failed to refresh data")
        }
    }

    /**
     * Clear cache for a specific workspace
     */
    suspend fun clearCache(workspaceId: String) {
        val currentUserId = getCurrentUserId() ?: return

        roleDao.deleteAllWorkspaceRolesForUser(currentUserId, workspaceId)
        permissionDao.deleteAllWorkspacePermissionsForUser(currentUserId, workspaceId)
    }

    /**
     * Check if roles and permissions are cached for a workspace
     */
    suspend fun isCached(workspaceId: String): Boolean {
        val currentUserId = getCurrentUserId() ?: return false

        val rolesCount = roleDao.getRolesCountForUser(currentUserId, workspaceId)
        val permissionsCount = permissionDao.getPermissionsCountForUser(currentUserId, workspaceId)

        return rolesCount > 0 && permissionsCount > 0
    }
}