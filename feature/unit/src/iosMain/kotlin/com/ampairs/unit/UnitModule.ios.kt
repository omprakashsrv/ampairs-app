package com.ampairs.unit

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.unit.data.db.UnitDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific Unit Module
 *
 * Provides platform-specific database configuration using:
 * - iOS Documents directory for writable storage
 * - WorkspaceAwareDatabaseFactory for workspace isolation
 *
 * CRITICAL: Uses factory scope (NOT single) for workspace isolation.
 * DatabaseScopeManager handles actual singleton behavior per workspace.
 */
val unitPlatformModule: Module = module {
    factory<UnitDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = UnitDatabase::class,
            moduleName = "unit"
        )
    }
}
