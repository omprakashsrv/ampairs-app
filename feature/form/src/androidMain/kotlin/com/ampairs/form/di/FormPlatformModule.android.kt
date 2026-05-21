package com.ampairs.form.di

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.form.data.db.FormDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

// Replaced Koin formPlatformModule (actual) for Android.
// FormDatabase is provided without @SingleIn (workspace-aware factory).

@ContributesTo(AppScope::class)
interface FormAndroidModule {
    companion object {
        @Provides
        fun provideFormDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context
        ): FormDatabase = factory.createAndroidDatabase(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "form"
        )
    }
}
