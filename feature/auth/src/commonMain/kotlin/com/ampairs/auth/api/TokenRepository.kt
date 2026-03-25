package com.ampairs.auth.api

interface TokenRepository {
    // Legacy single-user methods (for backward compatibility)
    fun getRefreshToken(): String?
    fun getAccessToken(): String?
    fun updateToken(accessToken: String, refreshToken: String?)
    fun clearTokens()

    // New multi-user methods
    suspend fun getRefreshTokenForUser(userId: String): String?
    suspend fun getAccessTokenForUser(userId: String): String?
    suspend fun updateTokenForUser(userId: String, accessToken: String, refreshToken: String?)
    suspend fun clearTokensForUser(userId: String)

    // User session management
    suspend fun getCurrentUserId(): String?
    suspend fun setCurrentUser(userId: String)
    suspend fun clearCurrentUser()
    suspend fun getActiveUsers(): List<String>
    suspend fun getAllAuthenticatedUsers(): List<String>
    suspend fun isUserAuthenticated(userId: String): Boolean
    suspend fun logoutUser(userId: String)
    suspend fun addAuthenticatedUser(userId: String, accessToken: String, refreshToken: String?)
    suspend fun createDummyUserSession(): String
    suspend fun updateDummySessionWithRealUser(
        realUserId: String,
        accessToken: String,
        refreshToken: String?
    )
    
    // Workspace management
    suspend fun getWorkspaceId(): String
}

/**
 * Extension function to check if any user is authenticated
 */
suspend fun TokenRepository.hasAnyAuthenticatedUser(): Boolean {
    return getActiveUsers().isNotEmpty()
}

/**
 * Extension function to check if current user is authenticated (legacy support)
 */
fun TokenRepository.isAuthenticated(): Boolean {
    val accessToken = getAccessToken()
    val refreshToken = getRefreshToken()
    return !accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank()
}