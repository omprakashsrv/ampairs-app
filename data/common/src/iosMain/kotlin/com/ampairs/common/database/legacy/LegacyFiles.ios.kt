package com.ampairs.common.database.legacy

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

internal actual fun legacyFileExists(path: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(path)

@OptIn(ExperimentalForeignApi::class)
internal actual fun deleteLegacyDatabaseFiles(path: String) {
    val fm = NSFileManager.defaultManager
    fm.removeItemAtPath(path, error = null)
    fm.removeItemAtPath("$path-wal", error = null)
    fm.removeItemAtPath("$path-shm", error = null)
}
