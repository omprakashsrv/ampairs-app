package com.ampairs.printing

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.printing.data.db.PrintingDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(WorkspaceScope::class)
interface PrintingAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrintingDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): PrintingDatabase = factory.createAndroidDatabase<PrintingDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "printing",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
