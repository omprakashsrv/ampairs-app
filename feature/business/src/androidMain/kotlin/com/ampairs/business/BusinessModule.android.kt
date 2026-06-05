package com.ampairs.business

import android.content.Context
import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import com.ampairs.business.data.db.migrations.BUSINESS_MIGRATION_2_3
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface BusinessAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideBusinessDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): BusinessDatabase {
            return factory.createAndroidDatabase<BusinessDatabase>(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "business",
                workspaceSlug = config.workspaceSlug,
                migrations = listOf(BUSINESS_MIGRATION_2_3),
            ).also { closableRegistry.register { it.close() } }
        }
    }
}
