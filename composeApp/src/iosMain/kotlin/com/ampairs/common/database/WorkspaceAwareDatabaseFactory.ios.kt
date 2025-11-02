package com.ampairs.common.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.workspace.WorkspaceContext

actual inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createPlatformDatabase(dbPath: String): T {
    // Extract workspace slug and module name from dbPath for scope management
    // Expected format: .../workspace_{slug}/customer.db
    val fileName = dbPath.substringAfterLast('/')  // customer.db
    val moduleName = fileName.removeSuffix(".db")  // customer

    val parentDir = dbPath.substringBeforeLast('/')  // .../workspace_{slug}
    val workspaceDirName = parentDir.substringAfterLast('/')  // workspace_{slug}

    // Extract slug from workspace_hardware-shop-ka
    val workspaceSlug = if (workspaceDirName.startsWith("workspace_")) {
        workspaceDirName.removePrefix("workspace_")  // hardware-shop-ka
    } else {
        WorkspaceContext.getCurrentWorkspaceSlugOrDefault()
    }

    println("iOSDatabaseFactory: Creating database for module=$moduleName, workspace=$workspaceSlug, path=$dbPath")

    val scopeManager = DatabaseScopeManager.getInstance()

    return scopeManager.getOrCreateDatabase(workspaceSlug, moduleName) {
        println("iOSDatabaseFactory: Building Room database: $fileName")
        Room.databaseBuilder<T>(
            name = dbPath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(queryDispatcher)
            .fallbackToDestructiveMigration(true)
            .build()
    }
}