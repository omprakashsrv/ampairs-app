package com.ampairs.common.database.legacy

import java.io.File

internal actual fun legacyFileExists(path: String): Boolean = File(path).exists()

internal actual fun deleteLegacyDatabaseFiles(path: String) {
    File(path).delete()
    File("$path-wal").delete()
    File("$path-shm").delete()
}
