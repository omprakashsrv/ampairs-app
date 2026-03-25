package com.ampairs.workspace.store

import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.UserInvitationApi
import com.ampairs.workspace.api.model.UserInvitationResponse
import com.ampairs.workspace.db.dao.UserInvitationDao
import com.ampairs.workspace.db.entity.UserInvitationEntity
import com.ampairs.workspace.domain.UserInvitation
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

// Store5 key for user invitations
data class UserInvitationKey(
    val userId: String,
    val refresh: Boolean = false
)

// Type aliases for Store5
typealias UserInvitationStore = Store<UserInvitationKey, List<UserInvitation>>

class UserInvitationStoreFactory(
    private val invitationApi: UserInvitationApi,
    private val invitationDao: UserInvitationDao,
) {

    fun create(): UserInvitationStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<UserInvitationKey, List<UserInvitation>> {
        return Fetcher.of { key ->
            val response = invitationApi.getPendingInvitations()
            if (response.data != null && response.error == null) {
                response.data!!.map { it.toDomainModel() }
            } else {
                throw Exception(response.error?.message ?: "Failed to fetch user invitations")
            }
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<UserInvitationKey, List<UserInvitation>, List<UserInvitation>> {
        return SourceOfTruth.of(
            reader = { key ->
                invitationDao.getUserInvitations(key.userId)
                    .map { entities ->
                        entities.map { it.toDomainModel() }
                    }
            },
            writer = { key, invitations ->
                // Convert Domain models to entities with sync metadata
                val currentTime = currentTimeMillis()
                val entities = invitations.map { invitation ->
                    invitation.toEntityModel(key.userId).copy(
                        sync_state = "SYNCED",
                        last_synced_at = currentTime,
                        server_updated_at = currentTime,
                        local_updated_at = currentTime
                    )
                }

                // Clear existing invitations and insert new ones
                invitationDao.deleteAllUserInvitations(key.userId)
                invitationDao.insertUserInvitations(entities)
            }
        )
    }
}

// Extension functions for data model conversions
private fun UserInvitationResponse.toDomainModel(): UserInvitation {
    return UserInvitation(
        id = this.id,
        workspaceId = this.workspaceId,
        workspaceName = this.workspaceName ?: "",
        workspaceDescription = this.workspaceDescription,
        workspaceType = this.workspaceType,
        role = this.role,
        message = this.message,
        invitedBy = this.invitedBy,
        inviterName = this.inviterName,
        expiresAt = this.expiresAt,
        createdAt = this.createdAt,
        daysUntilExpiry = this.daysUntilExpiry
    )
}

private fun UserInvitation.toEntityModel(userId: String): UserInvitationEntity {
    return UserInvitationEntity(
        id = this.id,
        user_id = userId,
        workspace_id = this.workspaceId,
        workspace_name = this.workspaceName,
        workspace_description = this.workspaceDescription,
        workspace_type = this.workspaceType,
        role = this.role,
        message = this.message,
        invited_by = this.invitedBy,
        inviter_name = this.inviterName,
        expires_at = this.expiresAt,
        created_at = this.createdAt,
        days_until_expiry = this.daysUntilExpiry
    )
}

private fun UserInvitationEntity.toDomainModel(): UserInvitation {
    return UserInvitation(
        id = this.id,
        workspaceId = this.workspace_id,
        workspaceName = this.workspace_name,
        workspaceDescription = this.workspace_description,
        workspaceType = this.workspace_type,
        role = this.role,
        message = this.message,
        invitedBy = this.invited_by,
        inviterName = this.inviter_name,
        expiresAt = this.expires_at,
        createdAt = this.created_at,
        daysUntilExpiry = this.days_until_expiry
    )
}