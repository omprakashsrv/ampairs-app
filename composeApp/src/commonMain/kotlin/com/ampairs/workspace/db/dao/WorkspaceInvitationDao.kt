package com.ampairs.workspace.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.db.entity.WorkspaceInvitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceInvitationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: WorkspaceInvitationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitations(invitations: List<WorkspaceInvitationEntity>)

    @Update
    suspend fun updateInvitation(invitation: WorkspaceInvitationEntity)

    @Query("SELECT * FROM workspaceInvitationEntity WHERE user_id = :userId ORDER BY created_at DESC")
    fun getAllInvitationsForUser(userId: String): Flow<List<WorkspaceInvitationEntity>>

    @Query("SELECT * FROM workspaceInvitationEntity WHERE user_id = :userId AND workspace_id = :workspaceId ORDER BY created_at DESC")
    fun getInvitationsForWorkspace(userId: String, workspaceId: String): Flow<List<WorkspaceInvitationEntity>>

    @Query("SELECT * FROM workspaceInvitationEntity WHERE user_id = :userId AND workspace_id = :workspaceId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun getInvitationsForWorkspacePaged(userId: String, workspaceId: String, limit: Int, offset: Int): Flow<List<WorkspaceInvitationEntity>>

    @Query("SELECT COUNT(*) FROM workspaceInvitationEntity WHERE user_id = :userId AND workspace_id = :workspaceId")
    suspend fun getInvitationCountForWorkspace(userId: String, workspaceId: String): Int

    @Query("SELECT * FROM workspaceInvitationEntity WHERE id = :id AND user_id = :userId")
    suspend fun getInvitationById(id: String, userId: String): WorkspaceInvitationEntity?

    @Query("SELECT * FROM workspaceInvitationEntity WHERE id = :id AND user_id = :userId")
    fun getInvitationByIdFlow(id: String, userId: String): Flow<WorkspaceInvitationEntity?>

    @Query("SELECT * FROM workspaceInvitationEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND status = :status ORDER BY created_at DESC")
    fun getInvitationsByStatus(userId: String, workspaceId: String, status: String): Flow<List<WorkspaceInvitationEntity>>

    @Query("SELECT * FROM workspaceInvitationEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND invited_role = :role ORDER BY created_at DESC")
    fun getInvitationsByRole(userId: String, workspaceId: String, role: String): Flow<List<WorkspaceInvitationEntity>>

    @Query("SELECT * FROM workspaceInvitationEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND (phone LIKE '%' || :query || '%' OR recipient_name LIKE '%' || :query || '%') ORDER BY created_at DESC")
    fun searchInvitations(userId: String, workspaceId: String, query: String): Flow<List<WorkspaceInvitationEntity>>

    @Query("SELECT * FROM workspaceInvitationEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND status = :status AND invited_role = :role ORDER BY created_at DESC")
    fun getInvitationsByStatusAndRole(userId: String, workspaceId: String, status: String, role: String): Flow<List<WorkspaceInvitationEntity>>

    @Query("UPDATE workspaceInvitationEntity SET status = :status, local_updated_at = :updatedAt, sync_state = 'PENDING_UPLOAD' WHERE id = :id AND user_id = :userId")
    suspend fun updateInvitationStatus(id: String, userId: String, status: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE workspaceInvitationEntity SET resend_count = resend_count + 1, local_updated_at = :updatedAt, sync_state = 'PENDING_UPLOAD' WHERE id = :id AND user_id = :userId")
    suspend fun incrementResendCount(id: String, userId: String, updatedAt: Long = currentTimeMillis())

    @Query("DELETE FROM workspaceInvitationEntity WHERE id = :id AND user_id = :userId")
    suspend fun deleteInvitation(id: String, userId: String)

    @Query("DELETE FROM workspaceInvitationEntity WHERE workspace_id = :workspaceId AND user_id = :userId")
    suspend fun deleteAllInvitationsForWorkspace(workspaceId: String, userId: String)

    @Query("DELETE FROM workspaceInvitationEntity WHERE user_id = :userId")
    suspend fun deleteAllInvitationsForUser(userId: String)

    // Sync-related queries
    @Query("SELECT * FROM workspaceInvitationEntity WHERE sync_state = :syncState")
    suspend fun getInvitationsBySyncState(syncState: String): List<WorkspaceInvitationEntity>

    @Query("UPDATE workspaceInvitationEntity SET sync_state = :syncState, last_synced_at = :syncTime WHERE id = :id AND user_id = :userId")
    suspend fun updateSyncState(id: String, userId: String, syncState: String, syncTime: Long)

    @Query("SELECT * FROM workspaceInvitationEntity WHERE sync_state IN ('PENDING_UPLOAD', 'FAILED')")
    suspend fun getPendingUploadInvitations(): List<WorkspaceInvitationEntity>

    @Query("SELECT COUNT(*) FROM workspaceInvitationEntity WHERE user_id = :userId AND workspace_id = :workspaceId AND status = :status")
    suspend fun getInvitationCountByStatus(userId: String, workspaceId: String, status: String): Int
}