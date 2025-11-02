package com.ampairs.workspace.db.dao

import androidx.room.*
import com.ampairs.workspace.db.entity.WorkspacePermissionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for workspace permissions with offline-first support
 */
@Dao
interface WorkspacePermissionDao {

    // Basic CRUD operations
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspacePermission(permission: WorkspacePermissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspacePermissions(permissions: List<WorkspacePermissionEntity>)

    @Update
    suspend fun updateWorkspacePermission(permission: WorkspacePermissionEntity)

    @Delete
    suspend fun deleteWorkspacePermission(permission: WorkspacePermissionEntity)

    // Query operations for offline-first support

    @Query("SELECT * FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId ORDER BY module ASC")
    fun getWorkspacePermissionsForUser(userId: String, workspaceId: String): Flow<List<WorkspacePermissionEntity>>

    @Query("SELECT * FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND module = :module")
    suspend fun getPermissionForModule(userId: String, workspaceId: String, module: String): WorkspacePermissionEntity?

    @Query("SELECT * FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun getWorkspacePermissionsListForUser(userId: String, workspaceId: String): List<WorkspacePermissionEntity>

    // Sync-related queries

    @Query("SELECT * FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND sync_state = :syncState")
    suspend fun getPermissionsBySyncState(userId: String, workspaceId: String, syncState: String): List<WorkspacePermissionEntity>

    @Query("UPDATE workspacePermissionEntity SET sync_state = :syncState, last_synced_at = :timestamp WHERE user_id = :userId AND workspace_id = :workspaceId AND module = :module")
    suspend fun updateSyncState(userId: String, workspaceId: String, module: String, syncState: String, timestamp: Long)

    @Query("UPDATE workspacePermissionEntity SET sync_state = :syncState, last_synced_at = :timestamp WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun updateAllSyncState(userId: String, workspaceId: String, syncState: String, timestamp: Long)

    // Cache management

    @Query("DELETE FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun deleteAllWorkspacePermissionsForUser(userId: String, workspaceId: String)

    @Query("DELETE FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND module = :module")
    suspend fun deletePermissionForModule(userId: String, workspaceId: String, module: String)

    @Query("SELECT COUNT(*) FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun getPermissionsCountForUser(userId: String, workspaceId: String): Int

    // Module-specific queries

    @Query("SELECT DISTINCT module FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId ORDER BY module ASC")
    suspend fun getAvailableModules(userId: String, workspaceId: String): List<String>

    @Query("SELECT * FROM workspacePermissionEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND module IN (:modules)")
    suspend fun getPermissionsForModules(userId: String, workspaceId: String, modules: List<String>): List<WorkspacePermissionEntity>
}