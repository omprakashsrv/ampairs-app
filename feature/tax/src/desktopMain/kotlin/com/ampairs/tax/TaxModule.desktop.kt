package com.ampairs.tax

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.tax.data.db.TaxRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.tax.data.db.migrations.TAX_MIGRATION_2_3

@ContributesTo(WorkspaceScope::class)
interface TaxDesktopModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideTaxDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): TaxRoomDatabase = factory.createDatabase<TaxRoomDatabase>(
            moduleName = "tax",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(TAX_MIGRATION_2_3),
        ).also { closableRegistry.register { it.close() } }
    }
}
