package com.ampairs.workspace

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.di.AppScope
import com.ampairs.workspace.db.WorkspaceRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface WorkspaceAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWorkspaceRoomDatabase(context: Context): WorkspaceRoomDatabase {
            val dbFile = context.getDatabasePath("workspace.db")
            return Room.databaseBuilder<WorkspaceRoomDatabase>(
                context = context,
                name = dbFile.absolutePath
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .enableMultiInstanceInvalidation()
                .build()
        }
    }
}
