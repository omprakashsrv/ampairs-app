package com.ampairs.tax

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.tax.data.db.TaxRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

// Replaced Koin taxPlatformModule for Android.
// TaxRoomDatabase is provided without @SingleIn (workspace-aware factory).

@ContributesTo(AppScope::class)
interface TaxAndroidModule {
    companion object {
        @Provides
        fun provideTaxDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context
        ): TaxRoomDatabase = factory.createAndroidDatabase(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "tax"
        )
    }
}
