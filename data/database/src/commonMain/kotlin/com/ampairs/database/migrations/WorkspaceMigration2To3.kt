package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v2 -> v3 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: adds the pull-only
 * `demand_forecast` mirror table (feature 022). It is a disposable server cache — the table is
 * created empty and populated on the next forecast pull — so this is a plain `CREATE TABLE` with no
 * data migration. Column types/indices mirror Room's generated schema for `DemandForecastEntity`.
 */
val WORKSPACE_MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `demand_forecast` (" +
                "`uid` TEXT NOT NULL, " +
                "`product_id` TEXT NOT NULL, " +
                "`period_start` TEXT NOT NULL, " +
                "`horizon` INTEGER NOT NULL, " +
                "`mean_qty` REAL NOT NULL, " +
                "`std_dev_qty` REAL NOT NULL, " +
                "`method` TEXT NOT NULL, " +
                "`confidence` TEXT NOT NULL, " +
                "`generated_at` TEXT, " +
                "`updated_at` TEXT, " +
                "PRIMARY KEY(`uid`))",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `demand_forecast_uid_idx` ON `demand_forecast` (`uid`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `demand_forecast_product_id_idx` ON `demand_forecast` (`product_id`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `demand_forecast_updated_at_idx` ON `demand_forecast` (`updated_at`)",
        )
    }
}
