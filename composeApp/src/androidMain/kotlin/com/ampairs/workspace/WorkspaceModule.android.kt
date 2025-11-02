package com.ampairs.workspace

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.workspace.db.WorkspaceRoomDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val workspacePlatformModule: Module = module {
    single<WorkspaceRoomDatabase> {
        val context = androidContext()
        val dbFile = context.getDatabasePath("workspace.db")
        Room.databaseBuilder<WorkspaceRoomDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}