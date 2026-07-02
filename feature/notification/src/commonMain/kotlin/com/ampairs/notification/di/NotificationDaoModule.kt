package com.ampairs.notification.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.notification.data.db.NotificationDatabase
import com.ampairs.notification.data.db.dao.NotificationDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface NotificationDaoModule {
    companion object {
        @Provides
        fun provideNotificationDao(db: NotificationDatabase): NotificationDao = db.notificationDao()
    }
}
