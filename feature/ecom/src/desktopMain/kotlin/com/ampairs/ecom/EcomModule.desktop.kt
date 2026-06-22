package com.ampairs.ecom

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.ecom.data.db.EcomRoomDatabase
import com.ampairs.ecom.data.db.migrations.ECOM_MIGRATION_1_2
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface EcomDesktopModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideEcomDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): EcomRoomDatabase = factory.createDatabase<EcomRoomDatabase>(
            moduleName = "ecom",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(ECOM_MIGRATION_1_2),
        ).also { closableRegistry.register { it.close() } }
    }
}
