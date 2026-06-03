package com.ampairs.unit

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.unit.data.db.UnitDatabase
import com.ampairs.unit.data.db.migrations.UNIT_MIGRATION_1_2
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.workspace.WorkspaceClosableRegistry

@ContributesTo(WorkspaceScope::class)
interface UnitIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideUnitDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): UnitDatabase = factory.createDatabase<UnitDatabase>(
            moduleName = "unit",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(UNIT_MIGRATION_1_2),
        ).also { closableRegistry.register { it.close() } }
    }
}
