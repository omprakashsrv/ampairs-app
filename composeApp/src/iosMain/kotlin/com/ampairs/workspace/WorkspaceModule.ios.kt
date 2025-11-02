package com.ampairs.workspace

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import com.ampairs.workspace.db.WorkspaceRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val workspacePlatformModule: Module = module {
    single<WorkspaceRoomDatabase> {
        Room.databaseBuilder<WorkspaceRoomDatabase>(
            name = getIosDatabasePath("workspace.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}