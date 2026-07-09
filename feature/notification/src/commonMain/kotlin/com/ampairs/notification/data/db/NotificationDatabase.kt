package com.ampairs.notification.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.ampairs.notification.data.db.dao.NotificationDao
import com.ampairs.notification.data.db.entity.NotificationLogEntity

/**
 * Notification Center Room Database.
 *
 * Workspace-isolated (created via WorkspaceAwareDatabaseFactory, moduleName "notification").
 *
 * Version: 1 (initial)
 * Entities: NotificationLogEntity
 */
@Database(
    entities = [
        NotificationLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(NotificationDatabaseConstructor::class)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object NotificationDatabaseConstructor : RoomDatabaseConstructor<NotificationDatabase> {
    override fun initialize(): NotificationDatabase
}
