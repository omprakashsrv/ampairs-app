package com.ampairs.agent

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.agent.data.db.AgentCatalogDatabase
import com.ampairs.common.desktop.DataDirectoryManager
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import java.io.File

/** App-scoped (global) model-catalog DB — shared across all workspaces; see the Android module. */
@ContributesTo(AppScope::class)
interface AgentCatalogDbDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideAgentCatalogDatabase(): AgentCatalogDatabase {
            val dbFile = File(DataDirectoryManager.getDatabaseDir(), "agent_catalog.db")
            return Room.databaseBuilder<AgentCatalogDatabase>(
                name = dbFile.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                // The catalog is a disposable cache (re-pulled from the manifest), so drop & recreate
                // on schema change instead of writing migrations.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
