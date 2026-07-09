package com.ampairs.workspace

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.di.AppScope
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.workspace.db.WorkspaceRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface WorkspaceIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceRoomDatabase(): WorkspaceRoomDatabase {
            return Room.databaseBuilder<WorkspaceRoomDatabase>(
                name = getIosDatabasePath("workspace.db")
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(DispatcherProvider.io)
                .build()
        }
    }
}
