package com.ampairs.workspace.store

import com.ampairs.common.model.PageResult
import com.ampairs.common.time.currentTimeMillis
import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.WorkspaceApiModel
import com.ampairs.workspace.api.model.WorkspaceListApiModel
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.db.entity.WorkspaceEntity
import com.ampairs.workspace.domain.Workspace
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

/**
 * Store5 Factory for Workspace data management with pagination support
 * Following Store5 guidelines for proper offline-first architecture
 */
typealias WorkspaceStore = Store<WorkspaceKey, PageResult<Workspace>>

/**
 * Pagination-aware WorkspaceKey for Store5
 * Each page is cached separately for optimal performance
 */
data class WorkspaceKey(
    val userId: String,
    val workspaceId: String? = null, // null for paginated list, non-null for single workspace
    val page: Int = 0,
    val size: Int = 10,
    val sortBy: String = "createdAt",
    val sortDir: String = "desc"
) {
    // Helper function to create key for single workspace
    companion object {
        fun forWorkspace(userId: String, workspaceId: String) = WorkspaceKey(
            userId = userId,
            workspaceId = workspaceId
        )
        
        fun forPage(userId: String, page: Int, size: Int = 10) = WorkspaceKey(
            userId = userId,
            page = page,
            size = size
        )
    }
}

class WorkspaceStoreFactory(
    private val workspaceApi: WorkspaceApi,
    private val workspaceDao: WorkspaceDao,
) {

    fun create(): WorkspaceStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<WorkspaceKey, PageResult<Workspace>> {
        return Fetcher.of { key ->
            val result: PageResult<Workspace> = if (key.workspaceId != null) {
                // Fetch single workspace
                val response = workspaceApi.getWorkspace(key.workspaceId)
                if (response.data != null && response.error == null) {
                    val workspace = response.data!!.toDomainModel()
                    // Return single workspace as PageResult
                    PageResult(
                        content = listOf(workspace),
                        totalElements = 1,
                        totalPages = 1,
                        currentPage = 0,
                        pageSize = 1,
                        isFirst = true,
                        isLast = true,
                        isEmpty = false
                    )
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch workspace")
                }
            } else {
                // Fetch paginated user workspaces
                val response = workspaceApi.getUserWorkspaces(
                    page = key.page,
                    size = key.size,
                    sortBy = key.sortBy,
                    sortDir = key.sortDir
                )
                if (response.data != null && response.error == null) {
                    val pagedResponse = response.data!!
                    val workspaces = pagedResponse.content.map { it.toDomainModel() }
                    
                    // Convert API PagedWorkspaceResponse to domain PageResult
                    PageResult(
                        content = workspaces,
                        totalElements = pagedResponse.totalElements,
                        totalPages = pagedResponse.totalPages,
                        currentPage = pagedResponse.pageNumber,
                        pageSize = pagedResponse.pageSize,
                        isFirst = pagedResponse.first,
                        isLast = pagedResponse.last,
                        isEmpty = pagedResponse.empty
                    )
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch workspaces")
                }
            }
            result
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<WorkspaceKey, PageResult<Workspace>, PageResult<Workspace>> {
        return SourceOfTruth.of(
            reader = { key ->
                if (key.workspaceId != null) {
                    // Read single workspace
                    workspaceDao.getWorkspaceByIdForUserFlow(key.workspaceId, key.userId)
                        .map { entity -> 
                            if (entity != null) {
                                val workspace = entity.toDomainModel()
                                PageResult(
                                    content = listOf(workspace),
                                    totalElements = 1,
                                    totalPages = 1,
                                    currentPage = 0,
                                    pageSize = 1,
                                    isFirst = true,
                                    isLast = true,
                                    isEmpty = false
                                )
                            } else {
                                PageResult(
                                    content = emptyList(),
                                    totalElements = 0,
                                    totalPages = 0,
                                    currentPage = 0,
                                    pageSize = key.size,
                                    isFirst = true,
                                    isLast = true,
                                    isEmpty = true
                                )
                            }
                        }
                } else {
                    // Read paginated user workspaces
                    workspaceDao.getWorkspacesPaged(key.userId, key.size, key.page * key.size)
                        .map { entities -> 
                            val workspaces = entities.map { it.toDomainModel() }
                            val totalCount = workspaceDao.getWorkspaceCountForUser(key.userId)
                            val totalPages = (totalCount + key.size - 1) / key.size
                            
                            PageResult(
                                content = workspaces,
                                totalElements = totalCount,
                                totalPages = totalPages,
                                currentPage = key.page,
                                pageSize = key.size,
                                isFirst = key.page == 0,
                                isLast = key.page >= totalPages - 1,
                                isEmpty = workspaces.isEmpty()
                            )
                        }
                }
            },
            writer = { key, pageResult ->
                // Convert Domain models to entities with sync metadata
                val currentTime = currentTimeMillis()
                val entities = pageResult.content.map { workspace ->
                    workspace.toEntityModel(key.userId).copy(
                        sync_state = "SYNCED",
                        last_synced_at = currentTime,
                        server_updated_at = currentTime,
                        local_updated_at = currentTime
                    )
                }
                
                if (key.workspaceId != null) {
                    // Single workspace - replace existing
                    workspaceDao.deleteWorkspaceForUser(key.workspaceId, key.userId)
                    workspaceDao.insertWorkspaces(entities)
                } else {
                    // Paginated data - only clear and insert if it's the first page
                    // For subsequent pages, just insert new data
                    if (key.page == 0) {
                        workspaceDao.deleteAllWorkspacesForUser(key.userId)
                    }
                    workspaceDao.insertWorkspaces(entities)
                }
            }
        )
    }

}

// Extension functions for data model conversions
private fun WorkspaceApiModel.toDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt,
        trialExpiresAt = this.trialExpiresAt,
        memberCount = this.memberCount,
        isTrial = this.isTrial,
        storagePercentage = this.storagePercentage
    )
}

private fun WorkspaceListApiModel.toDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        isActive = true, // Default for list view
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = 5, // Default from API
        storageLimitGb = 1, // Default from API
        storageUsedGb = 0, // Default from API
        timezone = "UTC", // Default from API
        language = "en", // Default from API
        createdBy = "", // Not available in list view
        createdAt = this.createdAt,
        updatedAt = this.createdAt, // Use createdAt as fallback
        lastActivityAt = this.lastActivityAt,
        trialExpiresAt = null, // Not available in list view
        memberCount = this.memberCount,
        isTrial = null, // Not available in list view
        storagePercentage = null // Not available in list view
    )
}

private fun WorkspaceApiModel.toEntityModel(userId: String): WorkspaceEntity {
    return WorkspaceEntity(
        id = this.id,
        user_id = userId,
        name = this.name,
        slug = this.slug,
        description = this.description ?: "",
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl ?: "",
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt ?: "",
        trialExpiresAt = this.trialExpiresAt ?: "",
        memberCount = this.memberCount ?: 0,
        isTrial = this.isTrial ?: false,
        storagePercentage = this.storagePercentage ?: 0.0,
        sync_state = "SYNCED",
        last_synced_at = currentTimeMillis(),
        local_updated_at = currentTimeMillis(),
        server_updated_at = currentTimeMillis()
    )
}

private fun WorkspaceEntity.toDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = if (this.description.isEmpty()) null else this.description,
        workspaceType = this.workspaceType,
        avatarUrl = if (this.avatarUrl.isEmpty()) null else this.avatarUrl,
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = if (this.lastActivityAt.isEmpty()) null else this.lastActivityAt,
        trialExpiresAt = if (this.trialExpiresAt.isEmpty()) null else this.trialExpiresAt,
        memberCount = if (this.memberCount == 0) null else this.memberCount,
        isTrial = if (this.isTrial) true else null,
        storagePercentage = if (this.storagePercentage == 0.0) null else this.storagePercentage
    )
}

private fun Workspace.toEntityModel(userId: String): WorkspaceEntity {
    return WorkspaceEntity(
        id = this.id,
        user_id = userId,
        name = this.name,
        slug = this.slug,
        description = this.description ?: "",
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl ?: "",
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt ?: "",
        trialExpiresAt = this.trialExpiresAt ?: "",
        memberCount = this.memberCount ?: 0,
        isTrial = this.isTrial ?: false,
        storagePercentage = this.storagePercentage ?: 0.0,
        sync_state = "SYNCED",
        last_synced_at = currentTimeMillis(),
        local_updated_at = currentTimeMillis(),
        server_updated_at = currentTimeMillis()
    )
}