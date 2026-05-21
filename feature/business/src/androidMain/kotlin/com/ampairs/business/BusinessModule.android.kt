package com.ampairs.business

import android.content.Context
import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface BusinessAndroidModule {
    companion object {
        @Provides
        fun provideBusinessDatabase(factory: WorkspaceAwareDatabaseFactory, context: Context): BusinessDatabase =
            factory.createAndroidDatabase(context = context, queryDispatcher = Dispatchers.IO, moduleName = "business")
    }
}
