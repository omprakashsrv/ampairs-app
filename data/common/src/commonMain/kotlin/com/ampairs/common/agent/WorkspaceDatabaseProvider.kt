package com.ampairs.common.agent

import androidx.room3.RoomDatabase

/**
 * Handle to the active workspace's Room database for code that needs a raw connection rather than
 * a DAO — today that's the [ModuleQueryExecutor]s, which run validated read-only SELECTs on a
 * reader connection.
 *
 * Executors inject this interface rather than a concrete database type so the same code runs against
 * whichever workspace database the host app uses. The composition root binds it in `WorkspaceScope`:
 * - the main app binds the consolidated `AmpairsWorkspaceDatabase` (all module tables in one file);
 * - the storefront apps (shared-ecom) bind their `EcomRoomDatabase`.
 */
fun interface WorkspaceDatabaseProvider {
    fun get(): RoomDatabase
}
