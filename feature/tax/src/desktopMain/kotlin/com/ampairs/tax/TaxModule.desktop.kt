package com.ampairs.tax

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.tax.data.db.TaxRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Replaced Koin taxPlatformModule for Desktop.
// TaxRoomDatabase is provided without @SingleIn (workspace-aware factory).

@ContributesTo(AppScope::class)
interface TaxDesktopModule {
    companion object {
        @Provides
        fun provideTaxDatabase(factory: WorkspaceAwareDatabaseFactory): TaxRoomDatabase =
            factory.createDatabase(
                klass = TaxRoomDatabase::class,
                moduleName = "tax"
            )
    }
}
