package com.ampairs.file

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.file.db.FileRoomDatabase
import com.ampairs.file.manager.FileManager
import com.ampairs.file.manager.IosFileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface FileIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideFileDatabase(factory: WorkspaceAwareDatabaseFactory): FileRoomDatabase =
            factory.createDatabase(
                klass = FileRoomDatabase::class,
                moduleName = "file",
                migrations = emptyList(),
            )

        @Provides
        fun provideFileManager(): FileManager = IosFileManager()
    }
}
