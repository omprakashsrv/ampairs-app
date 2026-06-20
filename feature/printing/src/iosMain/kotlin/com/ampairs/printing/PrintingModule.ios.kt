package com.ampairs.printing

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.printing.data.db.PrintingDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface PrintingIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePrintingDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): PrintingDatabase = factory.createDatabase<PrintingDatabase>(
            moduleName = "printing",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
