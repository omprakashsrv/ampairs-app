package com.ampairs.workspace.db

import com.ampairs.common.di.AppScope
import com.ampairs.workspace.api.UserInvitationApi
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.ampairs.workspace.api.model.InvitationActionResponse
import com.ampairs.workspace.db.dao.UserInvitationDao
import com.ampairs.workspace.db.entity.UserInvitationEntity
import com.ampairs.workspace.domain.UserInvitation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
class UserInvitationRepository(
    private val invitationDao: UserInvitationDao,
    private val invitationApi: UserInvitationApi,
) {

    fun observeUserInvitations(userId: String): Flow<List<UserInvitation>> =
        invitationDao.getUserInvitations(userId).map { entities -> entities.map { it.toDomain() } }

    suspend fun syncUserInvitations(userId: String) {
        try {
            val response = invitationApi.getPendingInvitations()
            if (response.data != null && response.error == null) {
                invitationDao.deleteAllUserInvitations(userId)
                val entities = response.data!!.map { apiModel ->
                    UserInvitationEntity(
                        id = apiModel.id,
                        user_id = userId,
                        workspace_id = apiModel.workspaceId,
                        workspace_name = apiModel.workspaceName ?: "",
                        workspace_description = apiModel.workspaceDescription,
                        workspace_type = apiModel.workspaceType,
                        role = apiModel.role,
                        message = apiModel.message,
                        invited_by = apiModel.invitedBy,
                        inviter_name = apiModel.inviterName,
                        expires_at = apiModel.expiresAt,
                        created_at = apiModel.createdAt,
                        sync_state = "SYNCED"
                    )
                }
                invitationDao.insertUserInvitations(entities)
            }
        } catch (_: Exception) {
            // Graceful failure — UI shows cached data
        }
    }

    suspend fun acceptInvitation(invitationId: String): Result<InvitationActionResponse> {
        return try {
            val response = invitationApi.acceptInvitation(invitationId)
            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to accept invitation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectInvitation(invitationId: String): Result<InvitationActionResponse> {
        return try {
            val response = invitationApi.rejectInvitation(invitationId)
            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to reject invitation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun UserInvitationEntity.toDomain() = UserInvitation(
        id = id,
        workspaceId = workspace_id,
        workspaceName = workspace_name,
        workspaceDescription = workspace_description,
        workspaceType = workspace_type,
        role = role,
        message = message,
        invitedBy = invited_by,
        inviterName = inviter_name,
        expiresAt = expires_at,
        createdAt = created_at,
        daysUntilExpiry = days_until_expiry
    )
}
