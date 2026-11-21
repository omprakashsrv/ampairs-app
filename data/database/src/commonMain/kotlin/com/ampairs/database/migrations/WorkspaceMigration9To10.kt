package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v9 -> v10 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: creates the
 * field-sales automation (SFA) tables, which previously lived in a standalone `sfa.db`. All are
 * synced from the backend `/sync` feeds and pulled fresh, so this is a pure create (no data copy).
 *
 * The CREATE statements are copied verbatim from Room's exported `10.json` (`createSql`), so
 * they match the generated schema exactly and pass Room's post-migration validation.
 */
val WORKSPACE_MIGRATION_9_10 = object : Migration(9, 10) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // sfa_attendance
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_attendance` (`id` TEXT NOT NULL, `rep_member_uid` TEXT NOT NULL, `check_in_at` TEXT, `check_in_latitude` REAL, `check_in_longitude` REAL, `check_out_at` TEXT, `check_out_latitude` REAL, `check_out_longitude` REAL, `status` TEXT NOT NULL, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_attendance_id_idx` ON `sfa_attendance` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_attendance_rep_idx` ON `sfa_attendance` (`rep_member_uid`)")
        // sfa_beat_outlets
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_beat_outlets` (`id` TEXT NOT NULL, `beat_uid` TEXT NOT NULL, `customer_uid` TEXT NOT NULL, `visit_sequence` INTEGER NOT NULL, `visit_day` TEXT, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_beat_outlet_id_idx` ON `sfa_beat_outlets` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_beat_outlet_beat_idx` ON `sfa_beat_outlets` (`beat_uid`)")
        // sfa_beats
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_beats` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `rep_member_uid` TEXT, `scheduled_days` TEXT, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_beat_id_idx` ON `sfa_beats` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_beat_rep_idx` ON `sfa_beats` (`rep_member_uid`)")
        // sfa_field_orders
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_field_orders` (`id` TEXT NOT NULL, `visit_uid` TEXT, `customer_uid` TEXT NOT NULL, `rep_member_uid` TEXT NOT NULL, `order_uid` TEXT, `amount` REAL NOT NULL, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_field_order_id_idx` ON `sfa_field_orders` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_field_order_customer_idx` ON `sfa_field_orders` (`customer_uid`)")
        // sfa_journey_plans
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_journey_plans` (`id` TEXT NOT NULL, `rep_member_uid` TEXT NOT NULL, `beat_uid` TEXT NOT NULL, `weekday` TEXT, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_journey_plan_id_idx` ON `sfa_journey_plans` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_journey_plan_rep_idx` ON `sfa_journey_plans` (`rep_member_uid`)")
        // sfa_leaves
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_leaves` (`id` TEXT NOT NULL, `rep_member_uid` TEXT NOT NULL, `leave_date` TEXT NOT NULL, `reason` TEXT, `marked_by` TEXT, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_leave_id_idx` ON `sfa_leaves` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_leave_rep_idx` ON `sfa_leaves` (`rep_member_uid`)")
        // sfa_planned_visits
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_planned_visits` (`id` TEXT NOT NULL, `journey_plan_uid` TEXT, `beat_uid` TEXT, `customer_uid` TEXT NOT NULL, `rep_member_uid` TEXT NOT NULL, `planned_date` TEXT NOT NULL, `status` TEXT NOT NULL, `visit_sequence` INTEGER NOT NULL, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_planned_visit_id_idx` ON `sfa_planned_visits` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_planned_visit_rep_idx` ON `sfa_planned_visits` (`rep_member_uid`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_planned_visit_date_idx` ON `sfa_planned_visits` (`planned_date`)")
        // sfa_visit_survey_responses
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_visit_survey_responses` (`id` TEXT NOT NULL, `visit_uid` TEXT NOT NULL, `rep_member_uid` TEXT, `responses` TEXT, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_visit_survey_id_idx` ON `sfa_visit_survey_responses` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_visit_survey_visit_idx` ON `sfa_visit_survey_responses` (`visit_uid`)")
        // sfa_visits
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sfa_visits` (`id` TEXT NOT NULL, `customer_uid` TEXT NOT NULL, `rep_member_uid` TEXT NOT NULL, `planned_visit_uid` TEXT, `outcome` TEXT NOT NULL, `latitude` REAL, `longitude` REAL, `distance_meters` REAL, `geo_fence_status` TEXT NOT NULL, `ad_hoc` INTEGER NOT NULL, `notes` TEXT, `order_uid` TEXT, `visited_at` TEXT NOT NULL, `active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, `updated_at` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sfa_visit_id_idx` ON `sfa_visits` (`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_visit_customer_idx` ON `sfa_visits` (`customer_uid`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `sfa_visit_rep_idx` ON `sfa_visits` (`rep_member_uid`)")
    }
}
