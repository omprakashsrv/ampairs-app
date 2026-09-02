package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v5 -> v6 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: creates the
 * maintenance-build tables (customer-specific `cb_*` feature). All tables are
 * server-authoritative and re-sync from the backend `/sync` feeds, so this is a pure create — no
 * data mapping. Column shapes + index names mirror Room's generated schema for the new entities.
 */
val WORKSPACE_MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // cb_employees
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cb_employees` (" +
                "`id` TEXT NOT NULL, `employee_no` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`role` TEXT NOT NULL, `email` TEXT, `mobile` TEXT, `reports_to_employee_id` TEXT, " +
                "`zonal_office_id` TEXT, `mapped_store_ids` TEXT, `user_id` TEXT, " +
                "`active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, " +
                "`updated_at` TEXT, `ref_id` TEXT, PRIMARY KEY(`id`))",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `cb_employee_id_idx` ON `cb_employees` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `cb_employee_zone_idx` ON `cb_employees` (`zonal_office_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `cb_employee_user_idx` ON `cb_employees` (`user_id`)")

        // cb_zonal_offices
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cb_zonal_offices` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `city` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, " +
                "`updated_at` TEXT, `ref_id` TEXT, PRIMARY KEY(`id`))",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `cb_zonal_office_id_idx` ON `cb_zonal_offices` (`id`)")

        // cb_stores
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cb_stores` (" +
                "`id` TEXT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `city` TEXT NOT NULL, " +
                "`zonal_office_id` TEXT NOT NULL, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, " +
                "`created_at` TEXT, `updated_at` TEXT, `ref_id` TEXT, PRIMARY KEY(`id`))",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `cb_store_id_idx` ON `cb_stores` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `cb_store_zone_idx` ON `cb_stores` (`zonal_office_id`)")

        // cb_pm_schedules
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cb_pm_schedules` (" +
                "`id` TEXT NOT NULL, `asset_category` TEXT NOT NULL, `task_name` TEXT NOT NULL, " +
                "`checklist` TEXT, `frequency_unit` TEXT NOT NULL, `frequency_interval` INTEGER NOT NULL, " +
                "`active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, " +
                "`updated_at` TEXT, `ref_id` TEXT, PRIMARY KEY(`id`))",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `cb_pm_schedule_id_idx` ON `cb_pm_schedules` (`id`)")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `cb_pm_schedule_category_idx` ON `cb_pm_schedules` (`asset_category`)",
        )

        // cb_pm_entries
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cb_pm_entries` (" +
                "`id` TEXT NOT NULL, `store_id` TEXT NOT NULL, `zonal_office_id` TEXT NOT NULL, " +
                "`asset_category` TEXT NOT NULL, `pm_schedule_id` TEXT, `source` TEXT NOT NULL, " +
                "`due_date` TEXT, `status` TEXT NOT NULL, `assigned_to_employee_id` TEXT, " +
                "`assisted_by_employee_ids` TEXT, `completed_at` TEXT, `completed_by_employee_id` TEXT, " +
                "`checklist_result` TEXT, `ticket_id` TEXT, `active` INTEGER NOT NULL, " +
                "`synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, `ref_id` TEXT, " +
                "PRIMARY KEY(`id`))",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `cb_pm_entry_id_idx` ON `cb_pm_entries` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `cb_pm_entry_zone_idx` ON `cb_pm_entries` (`zonal_office_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `cb_pm_entry_status_idx` ON `cb_pm_entries` (`status`)")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `cb_pm_entry_assignee_idx` ON `cb_pm_entries` (`assigned_to_employee_id`)",
        )

        // cb_tickets
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cb_tickets` (" +
                "`id` TEXT NOT NULL, `store_id` TEXT NOT NULL, `zonal_office_id` TEXT NOT NULL, " +
                "`asset_category` TEXT NOT NULL, `sub_category` TEXT NOT NULL, `description` TEXT, " +
                "`status` TEXT NOT NULL, `assigned_to_employee_id` TEXT, `assisted_by_employee_ids` TEXT, " +
                "`raised_by_employee_id` TEXT, `raised_at` TEXT, `resolved_at` TEXT, " +
                "`origin_pm_entry_id` TEXT, `suggested_spare_part` TEXT, `active` INTEGER NOT NULL, " +
                "`synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, `ref_id` TEXT, " +
                "PRIMARY KEY(`id`))",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `cb_ticket_id_idx` ON `cb_tickets` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `cb_ticket_zone_idx` ON `cb_tickets` (`zonal_office_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `cb_ticket_status_idx` ON `cb_tickets` (`status`)")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `cb_ticket_assignee_idx` ON `cb_tickets` (`assigned_to_employee_id`)",
        )

        // cb_asset_category_aliases
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cb_asset_category_aliases` (" +
                "`id` TEXT NOT NULL, `canonical` TEXT NOT NULL, `alias` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, " +
                "`updated_at` TEXT, `ref_id` TEXT, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `cb_asset_alias_id_idx` ON `cb_asset_category_aliases` (`id`)",
        )
    }
}
