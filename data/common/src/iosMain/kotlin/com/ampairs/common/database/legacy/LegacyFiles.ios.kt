package com.ampairs.common.database.legacy

import kotlinx.coroutines.runBlocking
import platform.Foundation.NSFileManager

internal actual fun runBlockingCompat(block: suspend () -> Unit) {
    runBlocking { block() }
}

internal actual fun legacyFileExists(path: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(path)

internal actual fun deleteLegacyDatabaseFiles(path: String) {
    val fm = NSFileManager.defaultManager
    fm.removeItemAtPath(path, error = null)
    fm.removeItemAtPath("$path-wal", error = null)
    fm.removeItemAtPath("$path-shm", error = null)
}
