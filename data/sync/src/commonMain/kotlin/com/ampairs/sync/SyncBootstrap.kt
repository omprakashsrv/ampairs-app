package com.ampairs.sync

/**
 * Bootstrap reconciliation hook, invoked by EventSyncBridge on every (re)connect and on the hourly
 * timer. The implementation (in `shared`) fetches the workspace sync checkpoints and hands them to
 * [CentralSyncService.reconcileCheckpoints]. Declared here so EventSyncBridge (data/event) can
 * depend on it without reaching into higher-level modules.
 */
fun interface SyncBootstrap {
    suspend fun reconcileOnConnect()
}
