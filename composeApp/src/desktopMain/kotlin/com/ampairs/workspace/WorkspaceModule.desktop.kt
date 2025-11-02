package com.ampairs.workspace

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.workspace.db.WorkspaceRoomDatabase
import getDatabaseDir
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

val workspacePlatformModule: Module = module {
    single<WorkspaceRoomDatabase> {
        val dbFile = File(getDatabaseDir(), "workspace.db")
        Room.databaseBuilder<WorkspaceRoomDatabase>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}