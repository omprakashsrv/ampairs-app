package com.ampairs.workspace

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.desktop.DataDirectoryManager
import com.ampairs.common.di.AppScope
import com.ampairs.workspace.db.WorkspaceRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface WorkspaceDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceRoomDatabase(): WorkspaceRoomDatabase {
            val dbFile = File(DataDirectoryManager.getDatabaseDir(), "workspace.db")
            return Room.databaseBuilder<WorkspaceRoomDatabase>(
                name = dbFile.absolutePath
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        }
    }
}
