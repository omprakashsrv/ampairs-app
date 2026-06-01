package com.ampairs.sync

import com.ampairs.sync.db.SyncStateDatabase

fun interface SyncDatabaseFactory {
    fun create(workspaceSlug: String): SyncStateDatabase
}
