package com.ampairs.workspace.db.dao

import androidx.room.*
import com.ampairs.workspace.db.entity.WorkspaceRoleEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for workspace roles with offline-first support
 */
@Dao
interface WorkspaceRoleDao {

    // Basic CRUD operations
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceRole(role: WorkspaceRoleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceRoles(roles: List<WorkspaceRoleEntity>)

    @Update
    suspend fun updateWorkspaceRole(role: WorkspaceRoleEntity)

    @Delete
    suspend fun deleteWorkspaceRole(role: WorkspaceRoleEntity)

    // Query operations for offline-first support

    @Query("SELECT * FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId ORDER BY level ASC")
    fun getWorkspaceRolesForUser(userId: String, workspaceId: String): Flow<List<WorkspaceRoleEntity>>

    @Query("SELECT * FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :roleId")
    suspend fun getWorkspaceRoleForUser(userId: String, workspaceId: String, roleId: String): WorkspaceRoleEntity?

    @Query("SELECT * FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND name = :roleName")
    suspend fun getWorkspaceRoleByName(userId: String, workspaceId: String, roleName: String): WorkspaceRoleEntity?

    // Sync-related queries

    @Query("SELECT * FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND sync_state = :syncState")
    suspend fun getRolesBySyncState(userId: String, workspaceId: String, syncState: String): List<WorkspaceRoleEntity>

    @Query("UPDATE workspaceRoleEntity SET sync_state = :syncState, last_synced_at = :timestamp WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :roleId")
    suspend fun updateSyncState(userId: String, workspaceId: String, roleId: String, syncState: String, timestamp: Long)

    @Query("UPDATE workspaceRoleEntity SET sync_state = :syncState, last_synced_at = :timestamp WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun updateAllSyncState(userId: String, workspaceId: String, syncState: String, timestamp: Long)

    // Cache management

    @Query("DELETE FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun deleteAllWorkspaceRolesForUser(userId: String, workspaceId: String)

    @Query("DELETE FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :roleId")
    suspend fun deleteWorkspaceRoleForUser(userId: String, workspaceId: String, roleId: String)

    @Query("SELECT COUNT(*) FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun getRolesCountForUser(userId: String, workspaceId: String): Int

    // System role queries

    @Query("SELECT * FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND is_system_role = 1 ORDER BY level ASC")
    suspend fun getSystemRolesForUser(userId: String, workspaceId: String): List<WorkspaceRoleEntity>

    @Query("SELECT * FROM workspaceRoleEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND is_system_role = 0 ORDER BY name ASC")
    suspend fun getCustomRolesForUser(userId: String, workspaceId: String): List<WorkspaceRoleEntity>
}