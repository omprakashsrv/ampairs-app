package com.ampairs.workspace.db

import com.ampairs.workspace.api.UserInvitationApi
import com.ampairs.workspace.api.model.InvitationActionResponse
import com.ampairs.workspace.domain.UserInvitation
import com.ampairs.workspace.store.UserInvitationKey
import com.ampairs.workspace.store.UserInvitationStore
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Repository for user invitation management with offline-first support using Store5
 */
class UserInvitationRepository(
    private val invitationStore: UserInvitationStore,
    private val invitationApi: UserInvitationApi,
) {

    /**
     * Get user invitations as a Flow with offline-first support
     */
    fun getUserInvitationsFlow(userId: String): Flow<StoreReadResponse<List<UserInvitation>>> {
        val key = UserInvitationKey(userId = userId)
        return invitationStore.stream(StoreReadRequest.cached(key, refresh = true))
    }

    /**
     * Refresh user invitations from network
     */
    suspend fun refreshUserInvitations(userId: String) {
        val key = UserInvitationKey(userId = userId, refresh = true)
        invitationStore.stream(StoreReadRequest.fresh(key)).collect {
            // Collect to trigger the refresh - the flow in ViewModel will pick up the changes
        }
    }

    /**
     * Accept a workspace invitation
     */
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

    /**
     * Reject a workspace invitation
     */
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
}