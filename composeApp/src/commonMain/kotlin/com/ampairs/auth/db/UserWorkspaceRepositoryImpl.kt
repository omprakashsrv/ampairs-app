package com.ampairs.auth.db

import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.entity.UserSessionEntity

class UserWorkspaceRepositoryImpl(
    private val userSessionDao: UserSessionDao,
) : UserWorkspaceRepository {

    private var currentUserId: String? = null

    // Multi-user methods
    override suspend fun getWorkspaceIdForUser(userId: String): String {
        return userSessionDao.selectByUserId(userId)?.workspace_id ?: ""
    }

    override suspend fun setWorkspaceIdForUser(userId: String, workspaceId: String) {
        val existingSession = userSessionDao.selectByUserId(userId)
        if (existingSession != null) {
            userSessionDao.updateWorkspaceId(userId, workspaceId)
        } else {
            userSessionDao.insert(
                UserSessionEntity(
                    user_id = userId,
                    workspace_id = workspaceId,
                    is_current = userId == getCurrentUserId()
                )
            )
        }
    }

    private suspend fun getCurrentUserId(): String? {
        if (currentUserId == null) {
            currentUserId = userSessionDao.getCurrentUserSession()?.user_id
        }
        return currentUserId
    }
}