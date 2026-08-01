package com.ampairs.notification.data.repository

import com.ampairs.notification.data.db.dao.NotificationDao
import com.ampairs.notification.data.db.entity.toAppNotification
import com.ampairs.notification.data.db.entity.toEntity
import com.ampairs.notification.domain.model.AppNotification
import com.ampairs.notification.util.NotificationLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for in-app notifications. The [com.ampairs.notification.data.api.NotificationApi]
 * is owned by [com.ampairs.notification.sync.NotificationSyncDelegate]; writes here persist to Room
 * as unsynced and flag NOTIFICATION as PENDING_PUSH so CentralSyncService runs the automatic bulk push.
 */
@OptIn(ExperimentalTime::class)
@Inject
class NotificationRepository(
    private val dao: NotificationDao,
    private val syncStateDao: SyncStateDao,
) {

    fun observeNotifications(): Flow<List<AppNotification>> =
        dao.observeActive().map { entities -> entities.map { it.toAppNotification() } }

    fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    suspend fun getByUid(uid: String): AppNotification? = dao.getByUid(uid)?.toAppNotification()

    /**
     * Insert/update a notification that arrived via a foreground FCM push. Stored as already-synced
     * (the server is the source of truth for the row content); only flag changes are ever pushed.
     */
    suspend fun upsertFromPush(notification: AppNotification): Result<AppNotification> {
        return try {
            dao.upsert(notification.toEntity().copy(synced = true))
            Result.success(notification)
        } catch (e: Exception) {
            NotificationLogger.e("NotificationRepository", "Failed to upsert pushed notification", e)
            Result.failure(e)
        }
    }

    /** Mark a notification read locally (unsynced) and flag for automatic bulk push. */
    suspend fun markRead(uid: String): Result<Unit> {
        return try {
            val existing = dao.getByUid(uid) ?: return Result.success(Unit)
            if (!existing.read) {
                dao.upsert(existing.copy(read = true, synced = false))
                markPending()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            NotificationLogger.e("NotificationRepository", "Failed to mark notification read", e)
            Result.failure(e)
        }
    }

    /** Mark every active notification read locally (unsynced) and flag for automatic bulk push. */
    suspend fun markAllRead(): Result<Unit> {
        return try {
            val updated = dao.markAllReadUnsynced()
            if (updated > 0) markPending()
            Result.success(Unit)
        } catch (e: Exception) {
            NotificationLogger.e("NotificationRepository", "Failed to mark all notifications read", e)
            Result.failure(e)
        }
    }

    /** Dismiss (soft-delete) a notification locally (active=false, unsynced) and flag for push. */
    suspend fun dismiss(uid: String): Result<Unit> {
        return try {
            val existing = dao.getByUid(uid) ?: return Result.success(Unit)
            dao.upsert(existing.copy(active = false, synced = false))
            markPending()
            Result.success(Unit)
        } catch (e: Exception) {
            NotificationLogger.e("NotificationRepository", "Failed to dismiss notification", e)
            Result.failure(e)
        }
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.NOTIFICATION, Clock.System.now().toEpochMilliseconds())
    }
}
