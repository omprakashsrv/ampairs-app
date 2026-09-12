package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v5 -> v6 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: adds the AI Ops
 * Manager audit tables (`aiops_finding`, `aiops_decision`, `aiops_feedback`). Purely additive —
 * existing rows are untouched. The DDL mirrors Room's generated schema for the entities in
 * `com.ampairs.aiops.db.entity` (epoch-millis Long timestamps; TEXT for the JSON/enum-name columns).
 */
val WORKSPACE_MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // aiops_finding
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `aiops_finding` (
                `id` TEXT NOT NULL,
                `capability` TEXT NOT NULL,
                `entity_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `field` TEXT,
                `status` TEXT NOT NULL,
                `band` TEXT,
                `summary` TEXT NOT NULL,
                `signals` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_aiops_finding_entity_type_entity_id` ON `aiops_finding` (`entity_type`, `entity_id`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_aiops_finding_capability_status` ON `aiops_finding` (`capability`, `status`)",
        )

        // aiops_decision
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `aiops_decision` (
                `id` TEXT NOT NULL,
                `finding_id` TEXT NOT NULL,
                `capability` TEXT NOT NULL,
                `entity_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `field` TEXT,
                `before_value` TEXT,
                `after_value` TEXT,
                `action` TEXT NOT NULL,
                `confidence` REAL NOT NULL,
                `confidence_contributors` TEXT,
                `risk_level` TEXT NOT NULL,
                `reason` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `reversible` INTEGER NOT NULL,
                `reverted_at` INTEGER,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_aiops_decision_entity_type_entity_id` ON `aiops_decision` (`entity_type`, `entity_id`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_aiops_decision_finding_id` ON `aiops_decision` (`finding_id`)",
        )

        // aiops_feedback
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `aiops_feedback` (
                `id` TEXT NOT NULL,
                `finding_id` TEXT NOT NULL,
                `capability` TEXT NOT NULL,
                `verdict` TEXT NOT NULL,
                `edited_value` TEXT,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_aiops_feedback_finding_id` ON `aiops_feedback` (`finding_id`)",
        )
    }
}
