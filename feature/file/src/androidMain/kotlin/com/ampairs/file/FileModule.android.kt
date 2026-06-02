package com.ampairs.file

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.file.db.FileRoomDatabase
import com.ampairs.file.manager.AndroidFileManager
import com.ampairs.file.manager.FileManager
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface FileAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideFileDatabase(factory: WorkspaceAwareDatabaseFactory, context: Context): FileRoomDatabase =
            factory.createAndroidDatabase(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "file",
                migrations = emptyList(),
            )

        @Provides
        fun provideFileManager(context: Context): FileManager = AndroidFileManager(context)
    }
}
