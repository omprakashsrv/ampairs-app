package com.ampairs.form.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.form.data.db.FormDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Replaced Koin formPlatformModule (actual) for Desktop.
// FormDatabase is provided without @SingleIn (workspace-aware factory).

@ContributesTo(AppScope::class)
interface FormDesktopModule {
    companion object {
        @Provides
        fun provideFormDatabase(factory: WorkspaceAwareDatabaseFactory): FormDatabase =
            factory.createDatabase(
                klass = FormDatabase::class,
                moduleName = "form"
            )
    }
}
