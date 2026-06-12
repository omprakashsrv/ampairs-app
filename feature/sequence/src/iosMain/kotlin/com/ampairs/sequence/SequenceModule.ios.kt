package com.ampairs.sequence

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.sequence.data.db.SequenceDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface SequenceIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideSequenceDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): SequenceDatabase = factory.createDatabase<SequenceDatabase>(
            moduleName = "sequence",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
