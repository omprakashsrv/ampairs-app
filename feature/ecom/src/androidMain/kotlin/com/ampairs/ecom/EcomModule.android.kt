package com.ampairs.ecom

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.ecom.data.db.EcomRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(WorkspaceScope::class)
interface EcomAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideEcomDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): EcomRoomDatabase = factory.createAndroidDatabase<EcomRoomDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "ecom",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
