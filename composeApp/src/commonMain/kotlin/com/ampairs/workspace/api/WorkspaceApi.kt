package com.ampairs.workspace.api

import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import com.ampairs.workspace.api.model.WorkspaceApiModel
import com.ampairs.workspace.api.model.PagedWorkspaceResponse

interface WorkspaceApi {

    /**
     * Get user's workspaces with pagination
     */
    suspend fun getUserWorkspaces(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
    ): Response<PagedWorkspaceResponse>

    /**
     * Get workspace by ID
     */
    suspend fun getWorkspace(workspaceId: String): Response<WorkspaceApiModel>

    /**
     * Get workspace by slug
     */
    suspend fun getWorkspaceBySlug(slug: String): Response<WorkspaceApiModel>

    /**
     * Create a new workspace
     */
    suspend fun createWorkspace(request: CreateWorkspaceRequest): Response<WorkspaceApiModel>

    /**
     * Update workspace
     */
    suspend fun updateWorkspace(workspaceId: String, request: UpdateWorkspaceRequest): Response<WorkspaceApiModel>

    /**
     * Check if slug is available
     */
    suspend fun checkSlugAvailability(slug: String): Response<Map<String, Boolean>>

    /**
     * Archive workspace
     */
    suspend fun archiveWorkspace(workspaceId: String): Response<String>

    /**
     * Get current user's role and permissions in workspace
     */
    suspend fun getMyRole(workspaceId: String): Response<Map<String, Any>>
}