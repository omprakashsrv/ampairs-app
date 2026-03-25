package com.ampairs.unit

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.unit.data.db.UnitDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific Unit Module
 *
 * Provides platform-specific database configuration using:
 * - Android Context
 * - Dispatchers.IO for query execution
 * - WorkspaceAwareDatabaseFactory for workspace isolation
 *
 * CRITICAL: Uses factory scope (NOT single) for workspace isolation.
 * DatabaseScopeManager handles actual singleton behavior per workspace.
 */
val unitPlatformModule: Module = module {
    factory<UnitDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createAndroidDatabase(
            context = androidContext(),
            queryDispatcher = Dispatchers.IO,
            moduleName = "unit"
        )
    }
}
