package com.ampairs.printing.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * Workspace-isolated printing database. Holds device-local printer config, routing, and the print-job
 * spool (never synced) plus workspace-synced templates. One file per workspace per device — created
 * via [com.ampairs.common.database.WorkspaceAwareDatabaseFactory].
 */
@Database(
    entities = [
        PrinterEntity::class,
        PrintRoutingEntity::class,
        PrintJobEntity::class,
        PrintTemplateEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(PrintingDatabaseConstructor::class)
abstract class PrintingDatabase : RoomDatabase() {
    abstract fun printerDao(): PrinterDao
    abstract fun printRoutingDao(): PrintRoutingDao
    abstract fun printJobDao(): PrintJobDao
    abstract fun printTemplateDao(): PrintTemplateDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PrintingDatabaseConstructor : RoomDatabaseConstructor<PrintingDatabase> {
    override fun initialize(): PrintingDatabase
}
