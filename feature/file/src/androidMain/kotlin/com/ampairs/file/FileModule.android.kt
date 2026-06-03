package com.ampairs.file

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.file.db.FileRoomDatabase
import com.ampairs.file.manager.AndroidFileManager
import com.ampairs.file.manager.FileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface FileAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideFileDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): FileRoomDatabase = factory.createAndroidDatabase<FileRoomDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "file",
            workspaceSlug = config.workspaceSlug,
            migrations = emptyList(),
        )

        @Provides
        fun provideFileManager(context: Context): FileManager = AndroidFileManager(context)
    }
}
