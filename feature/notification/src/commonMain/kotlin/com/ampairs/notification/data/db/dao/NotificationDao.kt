package com.ampairs.notification.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.notification.data.db.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notifications.
 *
 * Reactive Flow queries drive the UI; suspend queries serve the sync delegate / repository.
 */
@Dao
interface NotificationDao {

    /** Active notifications, newest first — UI list. */
    @Query("SELECT * FROM notifications WHERE active = 1 ORDER BY updated_at DESC")
    fun observeActive(): Flow<List<NotificationLogEntity>>

    /** Reactive unread badge count (active and not yet read). */
    @Query("SELECT COUNT(*) FROM notifications WHERE active = 1 AND read = 0")
    fun observeUnreadCount(): Flow<Int>

    /** One-time fetch by uid. */
    @Query("SELECT * FROM notifications WHERE uid = :uid")
    suspend fun getByUid(uid: String): NotificationLogEntity?

    /** All locally unsynced rows (read/dismiss flag changes pending push). */
    @Query("SELECT * FROM notifications WHERE synced = 0")
    suspend fun getUnsynced(): List<NotificationLogEntity>

    /**
     * Mark every active, unread notification read in one statement, flagging each unsynced so the
     * change is bulk-pushed. Returns the number of rows updated.
     */
    @Query("UPDATE notifications SET read = 1, synced = 0 WHERE active = 1 AND read = 0")
    suspend fun markAllReadUnsynced(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notifications: List<NotificationLogEntity>)

    /** Hard delete a single row (after a confirmed dismiss push, or a server DELETE on pull). */
    @Query("DELETE FROM notifications WHERE uid = :uid")
    suspend fun hardDelete(uid: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
