package com.ampairs.unit

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import com.ampairs.unit.data.db.UnitDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Replaced Koin unitPlatformModule for iOS.
// UnitDatabase is provided without @SingleIn (workspace-aware factory).

@ContributesTo(AppScope::class)
interface UnitIosModule {
    companion object {
        @Provides
        fun provideUnitDatabase(factory: WorkspaceAwareDatabaseFactory): UnitDatabase =
            factory.createDatabase(
                klass = UnitDatabase::class,
                moduleName = "unit"
            )
    }
}
