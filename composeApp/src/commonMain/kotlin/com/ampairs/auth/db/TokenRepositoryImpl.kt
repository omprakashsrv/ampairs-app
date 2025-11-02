package com.ampairs.auth.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserSessionEntity
import com.ampairs.auth.db.entity.UserTokenEntity
import kotlinx.coroutines.runBlocking
import com.ampairs.common.time.currentTimeMillis

class TokenRepositoryImpl(
    val userTokenDao: UserTokenDao,
    val userSessionDao: UserSessionDao,
    val userWorkspaceRepository: UserWorkspaceRepository
) : TokenRepository {

    private var currentUserId: String? = null

    // Legacy single-user methods (for backward compatibility)
    override fun getRefreshToken(): String? {
        return runBlocking {
            getCurrentUserId()?.let { userId ->
                getRefreshTokenForUser(userId)
            } ?: userTokenDao.selectById()?.refresh_token
        }
    }

    override fun getAccessToken(): String? {
        return runBlocking {
            getCurrentUserId()?.let { userId ->
                getAccessTokenForUser(userId)
            } ?: userTokenDao.selectById()?.access_token
        }
    }

    override fun updateToken(accessToken: String, refreshToken: String?) {
        runBlocking {
            getCurrentUserId()?.let { userId ->
                updateTokenForUser(userId, accessToken, refreshToken)
            } ?: run {
                // Fallback: store token with legacy ID for backward compatibility
                userTokenDao.insertUserToken(
                    UserTokenEntity(
                        seq_id = 0,
                        id = "1", // Legacy token ID
                        user_id = "", // Empty user_id for legacy tokens
                        refresh_token = refreshToken ?: "",
                        access_token = accessToken,
                        is_active = true,
                        last_used = currentTimeMillis()
                    )
                )
            }
        }
    }

    override fun clearTokens() {
        runBlocking {
            getCurrentUserId()?.let { userId ->
                clearTokensForUser(userId)
            }
        }
    }


    // New multi-user methods
    override suspend fun getRefreshTokenForUser(userId: String): String? {
        return userTokenDao.selectByUserId(userId)?.refresh_token
    }

    override suspend fun getAccessTokenForUser(userId: String): String? {
        return userTokenDao.selectByUserId(userId)?.access_token
    }

    override suspend fun updateTokenForUser(
        userId: String,
        accessToken: String,
        refreshToken: String?,
    ) {
        userTokenDao.insertUserToken(
            UserTokenEntity(
                seq_id = 0,
                id = generateTokenId(userId),
                user_id = userId,
                refresh_token = refreshToken ?: "",
                access_token = accessToken,
                is_active = true,
                last_used = currentTimeMillis()
            )
        )
    }

    override suspend fun clearTokensForUser(userId: String) {
        userTokenDao.clearTokensForUser(userId)
    }


    override suspend fun getCurrentUserId(): String? {
        if (currentUserId == null) {
            currentUserId = userSessionDao.getCurrentUserSession()?.user_id
        }
        return currentUserId
    }

    override suspend fun setCurrentUser(userId: String) {
        userSessionDao.clearCurrentUser()
        userSessionDao.setCurrentUser(userId)
        userTokenDao.activateUser(userId)
        currentUserId = userId
    }

    override suspend fun clearCurrentUser() {
        userSessionDao.clearCurrentUser()
        currentUserId = null
    }

    override suspend fun getActiveUsers(): List<String> {
        return userTokenDao.selectActiveUsers().map { it.user_id }
    }

    override suspend fun getAllAuthenticatedUsers(): List<String> {
        return userTokenDao.selectAll()
            .filter { it.access_token.isNotBlank() || it.refresh_token.isNotBlank() }
            .map { it.user_id }
    }

    override suspend fun isUserAuthenticated(userId: String): Boolean {
        val token = userTokenDao.selectByUserId(userId)
        return token != null &&
                (token.access_token.isNotBlank() || token.refresh_token.isNotBlank()) &&
                token.is_active
    }

    override suspend fun logoutUser(userId: String) {
        userTokenDao.deactivateUser(userId)
        userSessionDao.deleteByUserId(userId)
        if (currentUserId == userId) {
            currentUserId = null
        }
    }

    override suspend fun addAuthenticatedUser(
        userId: String,
        accessToken: String,
        refreshToken: String?,
    ) {
        updateTokenForUser(userId, accessToken, refreshToken)

        // Create or update user session
        val existingSession = userSessionDao.selectByUserId(userId)
        if (existingSession != null) {
            userSessionDao.updateLoginInfo(userId)
        } else {
            userSessionDao.insert(
                UserSessionEntity(
                    user_id = userId,
                    is_current = false,
                    workspace_id = "",
                    last_login = currentTimeMillis()
                )
            )
        }
    }

    private fun generateTokenId(userId: String): String {
        return "token_${userId}_${currentTimeMillis()}"
    }

    /**
     * Creates a dummy user session for authentication flow
     * This allows getCurrentUserId() to work during the auth process
     */
    override suspend fun createDummyUserSession(): String {
        val dummyUserId = "temp_user_${currentTimeMillis()}"

        // Clear any existing dummy sessions (previous login attempts)
        clearDummySessions()

        // Create new dummy session
        userSessionDao.insert(
            UserSessionEntity(
                user_id = dummyUserId,
                is_current = true,
                workspace_id = "",
                last_login = currentTimeMillis()
            )
        )

        // Set as current user for token operations
        currentUserId = dummyUserId
        return dummyUserId
    }

    /**
     * Updates the dummy session with real user data
     */
    override suspend fun updateDummySessionWithRealUser(
        realUserId: String,
        accessToken: String,
        refreshToken: String?,
    ) {
        val currentDummyId = getCurrentUserId()
        if (currentDummyId != null && currentDummyId.startsWith("temp_user_")) {
            // Delete the dummy session
            userSessionDao.deleteByUserId(currentDummyId)

            // Delete dummy tokens
            userTokenDao.deleteByUserId(currentDummyId)

            // Create real user session
            userSessionDao.insert(
                UserSessionEntity(
                    user_id = realUserId,
                    is_current = true,
                    workspace_id = "",
                    last_login = currentTimeMillis()
                )
            )

            // Store tokens for real user
            updateTokenForUser(realUserId, accessToken, refreshToken)

            // Update current user
            currentUserId = realUserId
        }
    }

    /**
     * Clears any existing dummy sessions from previous login attempts
     */
    private suspend fun clearDummySessions() {
        val allSessions = userSessionDao.selectAll()
        allSessions.filter { it.user_id.startsWith("temp_user_") }
            .forEach { dummySession ->
                userSessionDao.deleteByUserId(dummySession.user_id)
                userTokenDao.deleteByUserId(dummySession.user_id)
            }
    }

    override suspend fun getWorkspaceId(): String {
        val userId = getCurrentUserId()
        return if (userId != null) {
            userWorkspaceRepository.getWorkspaceIdForUser(userId)
        } else {
            ""
        }
    }
}