package com.ampairs.auth.api

interface UserWorkspaceRepository {

    // Multi-user methods
    suspend fun getWorkspaceIdForUser(userId: String): String
    suspend fun setWorkspaceIdForUser(userId: String, workspaceId: String)
}