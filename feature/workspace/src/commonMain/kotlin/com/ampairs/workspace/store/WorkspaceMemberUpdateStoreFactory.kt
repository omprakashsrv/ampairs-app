package com.ampairs.workspace.store

import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.MemberDetailsResponse
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

/**
 * Store5 Factory for WorkspaceMember UPDATE operations
 * Handles member role/permission updates with proper sync
 */
typealias WorkspaceMemberUpdateStore = Store<WorkspaceMemberUpdateKey, Boolean>

data class WorkspaceMemberUpdateKey(
    val workspaceId: String,
    val memberId: String
)

data class WorkspaceMemberUpdateRequest(
    val key: WorkspaceMemberUpdateKey,
    val updateRequest: UpdateMemberRequest
)

class WorkspaceMemberUpdateStoreFactory(
    private val memberApi: WorkspaceMemberApi,
    private val memberDao: WorkspaceMemberDao,
) {

    fun create(): WorkspaceMemberUpdateStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<WorkspaceMemberUpdateKey, Boolean> {
        // For update operations, we don't typically fetch
        return Fetcher.of { _ -> true }
    }

    private fun createSourceOfTruth(): SourceOfTruth<WorkspaceMemberUpdateKey, Boolean, Boolean> {
        return SourceOfTruth.of(
            reader = { key ->
                // Check if there are pending updates
                memberDao.getWorkspaceMemberForUserFlow("", key.workspaceId, key.memberId)
                    .map { entity -> 
                        entity?.sync_state != "PENDING_UPLOAD"
                    }
            },
            writer = { key, success ->
                if (success) {
                    // Mark member as synced
                    memberDao.updateSyncState("", key.workspaceId, key.memberId, "SYNCED")
                    memberDao.updateLastSyncedAt("", key.workspaceId, key.memberId, currentTimeMillis())
                    memberDao.updatePendingChanges("", key.workspaceId, key.memberId, "")
                    memberDao.updateRetryCount("", key.workspaceId, key.memberId, 0)
                }
            }
        )
    }

    // Direct remove method for member removal operations
    suspend fun removeMember(key: WorkspaceMemberUpdateKey): Boolean {
        return try {
            val response = memberApi.removeMember(
                workspaceId = key.workspaceId,
                memberId = key.memberId
            )
            
            if (response.data != null && response.error == null) {
                // Remove from local cache
                removeLocalMember(key)
                true
            } else {
                throw Exception(response.error?.message ?: "Failed to remove member")
            }
        } catch (e: Exception) {
            // Mark as pending deletion
            markMemberForDeletion(key, e.message)
            throw e
        }
    }

    // Direct update method for now - can be enhanced later with proper Store5 updater
    suspend fun updateMember(key: WorkspaceMemberUpdateKey, request: UpdateMemberRequest): Boolean {
        return try {
            val response = memberApi.updateMember(
                workspaceId = key.workspaceId,
                memberId = key.memberId,
                request = request
            )
            
            if (response.data != null && response.error == null) {
                // Update local cache with fresh data
                val memberDetails = response.data!!
                updateLocalMember(key, memberDetails)
                true
            } else {
                throw Exception(response.error?.message ?: "Failed to update member")
            }
        } catch (e: Exception) {
            // Mark as pending retry
            markMemberForRetry(key, request, e.message)
            throw e
        }
    }

    private suspend fun updateLocalMember(key: WorkspaceMemberUpdateKey, memberDetails: MemberDetailsResponse) {
        val entity = memberDao.getWorkspaceMemberForUser("", key.workspaceId, key.memberId)
        entity?.let { existing ->
            val updated = existing.copy(
                name = memberDetails.name,
                role = memberDetails.role,
                status = memberDetails.status,
                permissions = Json.encodeToString(memberDetails.permissions),
                sync_state = "SYNCED",
                last_synced_at = currentTimeMillis(),
                server_updated_at = currentTimeMillis(),
                pending_changes = "",
                retry_count = 0
            )
            memberDao.insertWorkspaceMember(updated)
        }
    }

    private suspend fun removeLocalMember(key: WorkspaceMemberUpdateKey) {
        memberDao.deleteWorkspaceMemberForUser("", key.workspaceId, key.memberId)
    }

    private suspend fun markMemberForDeletion(key: WorkspaceMemberUpdateKey, errorMessage: String?) {
        val entity = memberDao.getWorkspaceMemberForUser("", key.workspaceId, key.memberId)
        entity?.let { existing ->
            val updated = existing.copy(
                sync_state = "PENDING_DELETE",
                retry_count = existing.retry_count + 1,
                local_updated_at = currentTimeMillis(),
                conflict_data = errorMessage ?: ""
            )
            memberDao.insertWorkspaceMember(updated)
        }
    }

    private suspend fun markMemberForRetry(
        key: WorkspaceMemberUpdateKey, 
        request: UpdateMemberRequest, 
        errorMessage: String?
    ) {
        val entity = memberDao.getWorkspaceMemberForUser("", key.workspaceId, key.memberId)
        entity?.let { existing ->
            val updated = existing.copy(
                sync_state = "PENDING_UPLOAD",
                pending_changes = Json.encodeToString(UpdateMemberRequest.serializer(), request),
                retry_count = existing.retry_count + 1,
                local_updated_at = currentTimeMillis(),
                conflict_data = errorMessage ?: ""
            )
            memberDao.insertWorkspaceMember(updated)
        }
    }
}