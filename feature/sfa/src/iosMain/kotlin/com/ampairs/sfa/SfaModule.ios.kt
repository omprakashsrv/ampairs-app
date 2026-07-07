package com.ampairs.sfa

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.sfa.data.db.SfaDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface SfaIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideSfaDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): SfaDatabase = factory.createDatabase<SfaDatabase>(
            moduleName = "sfa",
            workspaceSlug = config.workspaceSlug,
            migrations = emptyList(),
        ).also { closableRegistry.register { it.close() } }
    }
}
