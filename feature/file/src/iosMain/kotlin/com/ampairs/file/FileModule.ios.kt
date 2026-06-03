package com.ampairs.file

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.file.db.FileRoomDatabase
import com.ampairs.file.manager.FileManager
import com.ampairs.file.manager.IosFileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface FileIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideFileDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): FileRoomDatabase = factory.createDatabase<FileRoomDatabase>(
            moduleName = "file",
            workspaceSlug = config.workspaceSlug,
            migrations = emptyList(),
        )

        @Provides
        fun provideFileManager(): FileManager = IosFileManager()
    }
}
