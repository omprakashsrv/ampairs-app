package com.ampairs.agent

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.agent.data.db.AgentCatalogDatabase
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.di.AppScope
import com.ampairs.common.platform.getIosDatabasePath
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** App-scoped (global) model-catalog DB — shared across all workspaces; see the Android module. */
@ContributesTo(AppScope::class)
interface AgentCatalogDbIosModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideAgentCatalogDatabase(): AgentCatalogDatabase =
            Room.databaseBuilder<AgentCatalogDatabase>(
                name = getIosDatabasePath("agent_catalog.db"),
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(DispatcherProvider.io)
                // The catalog is a disposable cache (re-pulled from the manifest), so drop & recreate
                // on schema change instead of writing migrations.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
