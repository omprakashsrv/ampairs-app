package com.ampairs.common.platform

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Helper function to get proper iOS database file paths
 */
fun getIosDatabasePath(databaseName: String): String {
    val documentsPath = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).first() as String
    return "$documentsPath/$databaseName"
}