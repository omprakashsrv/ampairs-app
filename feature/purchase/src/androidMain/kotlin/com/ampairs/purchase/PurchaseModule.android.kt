package com.ampairs.purchase

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.purchase.db.PurchaseDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(WorkspaceScope::class)
interface PurchaseAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePurchaseDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): PurchaseDatabase = factory.createAndroidDatabase<PurchaseDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "purchase",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
