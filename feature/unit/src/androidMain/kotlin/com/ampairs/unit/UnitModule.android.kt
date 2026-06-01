package com.ampairs.unit

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.unit.data.db.UnitDatabase
import com.ampairs.unit.data.db.migrations.UNIT_MIGRATION_1_2
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

// Replaced Koin unitPlatformModule for Android.
// UnitDatabase is provided without @SingleIn (workspace-aware factory).

@ContributesTo(AppScope::class)
interface UnitAndroidModule {
    companion object {
        @Provides
        fun provideUnitDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context
        ): UnitDatabase = factory.createAndroidDatabase(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "unit",
            migrations = listOf(UNIT_MIGRATION_1_2)
        )
    }
}
