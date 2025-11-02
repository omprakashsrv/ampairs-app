package com.ampairs.workspace.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.workspace.db.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaces(workspaces: List<WorkspaceEntity>)

    @Query("SELECT * FROM workspaceEntity ORDER BY createdAt DESC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaceEntity WHERE user_id = :userId ORDER BY createdAt DESC")
    fun getAllWorkspacesForUser(userId: String): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaceEntity WHERE user_id = :userId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getWorkspacesPaged(userId: String, limit: Int, offset: Int): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaceEntity WHERE id = :id")
    suspend fun getWorkspaceById(id: String): WorkspaceEntity?

    @Query("SELECT * FROM workspaceEntity WHERE id = :id AND user_id = :userId")
    suspend fun getWorkspaceByIdForUser(id: String, userId: String): WorkspaceEntity?

    @Query("SELECT * FROM workspaceEntity WHERE slug = :slug")
    suspend fun getWorkspaceBySlug(slug: String): WorkspaceEntity?

    @Query("SELECT * FROM workspaceEntity WHERE slug = :slug AND user_id = :userId")
    suspend fun getWorkspaceBySlugForUser(slug: String, userId: String): WorkspaceEntity?

    @Query("SELECT * FROM workspaceEntity WHERE name LIKE '%' || :searchQuery || '%' ORDER BY createdAt DESC")
    fun searchWorkspaces(searchQuery: String): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaceEntity WHERE user_id = :userId AND name LIKE '%' || :searchQuery || '%' ORDER BY createdAt DESC")
    fun searchWorkspacesForUser(userId: String, searchQuery: String): Flow<List<WorkspaceEntity>>

    @Query("DELETE FROM workspaceEntity WHERE id = :id")
    suspend fun deleteWorkspace(id: String)

    @Query("DELETE FROM workspaceEntity WHERE id = :id AND user_id = :userId")
    suspend fun deleteWorkspaceForUser(id: String, userId: String)

    @Query("DELETE FROM workspaceEntity")
    suspend fun deleteAllWorkspaces()

    @Query("DELETE FROM workspaceEntity WHERE user_id = :userId")
    suspend fun deleteAllWorkspacesForUser(userId: String)

    @Query("SELECT COUNT(*) FROM workspaceEntity")
    suspend fun getWorkspaceCount(): Int

    @Query("SELECT COUNT(*) FROM workspaceEntity WHERE user_id = :userId")
    suspend fun getWorkspaceCountForUser(userId: String): Int
    
    // Store5 specific methods
    @Query("SELECT * FROM workspaceEntity WHERE id = :id AND user_id = :userId")
    fun getWorkspaceByIdForUserFlow(id: String, userId: String): Flow<WorkspaceEntity?>
    
    @Query("SELECT * FROM workspaceEntity WHERE user_id = :userId AND sync_state IN (:syncStates)")
    fun getWorkspacesWithSyncState(userId: String, syncStates: List<String>): Flow<List<WorkspaceEntity>>
    
    @Query("SELECT * FROM workspaceEntity WHERE sync_state = 'PENDING_UPLOAD' OR sync_state = 'FAILED'")
    fun getAllPendingSyncWorkspaces(): Flow<List<WorkspaceEntity>>
    
    @Query("SELECT * FROM workspaceEntity WHERE sync_state = 'CONFLICTED'")
    fun getAllConflictedWorkspaces(): Flow<List<WorkspaceEntity>>
    
    @Query("UPDATE workspaceEntity SET sync_state = :syncState WHERE id = :id AND user_id = :userId")
    suspend fun updateSyncState(id: String, userId: String, syncState: String)
    
    @Query("UPDATE workspaceEntity SET retry_count = :retryCount WHERE id = :id AND user_id = :userId")
    suspend fun updateRetryCount(id: String, userId: String, retryCount: Int)
}