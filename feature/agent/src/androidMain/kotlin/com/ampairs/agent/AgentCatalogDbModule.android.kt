package com.ampairs.agent

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.agent.data.db.AgentCatalogDatabase
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

/**
 * App-scoped (global) model-catalog DB. Built directly via Room — not the workspace factory — so it
 * survives workspace switches and is shared by all workspaces, matching the global model files.
 */
@ContributesTo(AppScope::class)
interface AgentCatalogDbAndroidModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideAgentCatalogDatabase(context: Context): AgentCatalogDatabase {
            val dbFile = context.getDatabasePath("agent_catalog.db")
            return Room.databaseBuilder<AgentCatalogDatabase>(
                context = context,
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
