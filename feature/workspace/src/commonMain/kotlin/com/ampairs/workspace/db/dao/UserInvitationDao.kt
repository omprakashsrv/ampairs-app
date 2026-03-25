package com.ampairs.workspace.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.workspace.db.entity.UserInvitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInvitationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInvitation(invitation: UserInvitationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInvitations(invitations: List<UserInvitationEntity>)

    @Query("SELECT * FROM userInvitationEntity WHERE user_id = :userId ORDER BY created_at DESC")
    fun getUserInvitations(userId: String): Flow<List<UserInvitationEntity>>

    @Query("SELECT * FROM userInvitationEntity WHERE user_id = :userId AND id = :invitationId")
    suspend fun getUserInvitation(userId: String, invitationId: String): UserInvitationEntity?

    @Query("SELECT * FROM userInvitationEntity WHERE user_id = :userId AND id = :invitationId")
    fun getUserInvitationFlow(userId: String, invitationId: String): Flow<UserInvitationEntity?>

    @Query("DELETE FROM userInvitationEntity WHERE user_id = :userId AND id = :invitationId")
    suspend fun deleteUserInvitation(userId: String, invitationId: String)

    @Query("DELETE FROM userInvitationEntity WHERE user_id = :userId")
    suspend fun deleteAllUserInvitations(userId: String)

    @Query("SELECT COUNT(*) FROM userInvitationEntity WHERE user_id = :userId")
    suspend fun getUserInvitationCount(userId: String): Int

    // Store5 specific methods for sync state management
    @Query("SELECT * FROM userInvitationEntity WHERE user_id = :userId AND sync_state IN (:syncStates)")
    fun getUserInvitationsWithSyncState(userId: String, syncStates: List<String>): Flow<List<UserInvitationEntity>>

    @Query("SELECT * FROM userInvitationEntity WHERE sync_state = 'PENDING_UPLOAD' OR sync_state = 'FAILED'")
    fun getAllPendingSyncInvitations(): Flow<List<UserInvitationEntity>>

    @Query("SELECT * FROM userInvitationEntity WHERE sync_state = 'CONFLICTED'")
    fun getAllConflictedInvitations(): Flow<List<UserInvitationEntity>>

    @Query("UPDATE userInvitationEntity SET sync_state = :syncState WHERE user_id = :userId AND id = :invitationId")
    suspend fun updateSyncState(userId: String, invitationId: String, syncState: String)

    @Query("UPDATE userInvitationEntity SET retry_count = :retryCount WHERE user_id = :userId AND id = :invitationId")
    suspend fun updateRetryCount(userId: String, invitationId: String, retryCount: Int)

    @Query("UPDATE userInvitationEntity SET last_synced_at = :timestamp WHERE user_id = :userId AND id = :invitationId")
    suspend fun updateLastSyncedAt(userId: String, invitationId: String, timestamp: Long)

    @Query("UPDATE userInvitationEntity SET pending_changes = :changes WHERE user_id = :userId AND id = :invitationId")
    suspend fun updatePendingChanges(userId: String, invitationId: String, changes: String)

    @Query("UPDATE userInvitationEntity SET conflict_data = :conflictData WHERE user_id = :userId AND id = :invitationId")
    suspend fun updateConflictData(userId: String, invitationId: String, conflictData: String)
}