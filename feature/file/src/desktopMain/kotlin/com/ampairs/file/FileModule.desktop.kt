package com.ampairs.file

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.file.db.FileRoomDatabase
import com.ampairs.file.manager.DesktopFileManager
import com.ampairs.file.manager.FileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface FileDesktopModule {
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
        fun provideFileManager(): FileManager = DesktopFileManager()
    }
}
