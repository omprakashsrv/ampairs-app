package com.ampairs.workspace.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.workspace.db.entity.WorkspaceMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceMember(member: WorkspaceMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceMembers(members: List<WorkspaceMemberEntity>)

    @Query("SELECT * FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId ORDER BY joined_at DESC")
    fun getWorkspaceMembersForUser(userId: String, workspaceId: String): Flow<List<WorkspaceMemberEntity>>

    @Query("SELECT * FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId ORDER BY joined_at DESC LIMIT :limit OFFSET :offset")
    fun getWorkspaceMembersPaged(userId: String, workspaceId: String, limit: Int, offset: Int): Flow<List<WorkspaceMemberEntity>>

    @Query("SELECT * FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :memberId")
    suspend fun getWorkspaceMemberForUser(userId: String, workspaceId: String, memberId: String): WorkspaceMemberEntity?

    @Query("SELECT * FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :memberId")
    fun getWorkspaceMemberForUserFlow(userId: String, workspaceId: String, memberId: String): Flow<WorkspaceMemberEntity?>

    @Query("SELECT * FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND (name LIKE '%' || :searchQuery || '%' OR email LIKE '%' || :searchQuery || '%') ORDER BY joined_at DESC")
    fun searchWorkspaceMembersForUser(userId: String, workspaceId: String, searchQuery: String): Flow<List<WorkspaceMemberEntity>>

    @Query("DELETE FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :memberId")
    suspend fun deleteWorkspaceMemberForUser(userId: String, workspaceId: String, memberId: String)

    @Query("DELETE FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun deleteAllWorkspaceMembersForUser(userId: String, workspaceId: String)

    @Query("DELETE FROM workspaceMemberEntity WHERE user_id = :userId")
    suspend fun deleteAllMembersForUser(userId: String)

    @Query("SELECT COUNT(*) FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun getWorkspaceMemberCountForUser(userId: String, workspaceId: String): Int

    // Store5 specific methods for sync state management
    @Query("SELECT * FROM workspaceMemberEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND sync_state IN (:syncStates)")
    fun getWorkspaceMembersWithSyncState(userId: String, workspaceId: String, syncStates: List<String>): Flow<List<WorkspaceMemberEntity>>

    @Query("SELECT * FROM workspaceMemberEntity WHERE sync_state = 'PENDING_UPLOAD' OR sync_state = 'FAILED'")
    fun getAllPendingSyncMembers(): Flow<List<WorkspaceMemberEntity>>

    @Query("SELECT * FROM workspaceMemberEntity WHERE sync_state = 'CONFLICTED'")
    fun getAllConflictedMembers(): Flow<List<WorkspaceMemberEntity>>

    @Query("UPDATE workspaceMemberEntity SET sync_state = :syncState WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :memberId")
    suspend fun updateSyncState(userId: String, workspaceId: String, memberId: String, syncState: String)

    @Query("UPDATE workspaceMemberEntity SET retry_count = :retryCount WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :memberId")
    suspend fun updateRetryCount(userId: String, workspaceId: String, memberId: String, retryCount: Int)

    @Query("UPDATE workspaceMemberEntity SET last_synced_at = :timestamp WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :memberId")
    suspend fun updateLastSyncedAt(userId: String, workspaceId: String, memberId: String, timestamp: Long)

    @Query("UPDATE workspaceMemberEntity SET pending_changes = :changes WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :memberId")
    suspend fun updatePendingChanges(userId: String, workspaceId: String, memberId: String, changes: String)

    @Query("UPDATE workspaceMemberEntity SET conflict_data = :conflictData WHERE user_id = :userId AND workspace_id = :workspaceId AND id = :memberId")
    suspend fun updateConflictData(userId: String, workspaceId: String, memberId: String, conflictData: String)
}