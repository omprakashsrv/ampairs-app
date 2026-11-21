package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v10 -> v11 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: creates the
 * communication tables (templates, bindings, schedules, campaigns, preferences + the pull-only
 * delivery-log cache), which previously lived in a standalone `communication.db`. All are synced
 * from the backend `/sync` feeds, so this is a pure create (no data copy).
 *
 * The CREATE statements are copied verbatim from Room's exported `11.json` (`createSql`), so
 * they match the generated schema exactly and pass Room's post-migration validation.
 */
val WORKSPACE_MIGRATION_10_11 = object : Migration(10, 11) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // comm_bindings
        connection.execSQL("CREATE TABLE IF NOT EXISTS `comm_bindings` (`uid` TEXT NOT NULL, `event_type` TEXT NOT NULL, `template_uid` TEXT NOT NULL, `channels` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `active` INTEGER NOT NULL, `updated_at` TEXT, PRIMARY KEY(`uid`))")
        // comm_campaigns
        connection.execSQL("CREATE TABLE IF NOT EXISTS `comm_campaigns` (`uid` TEXT NOT NULL, `name` TEXT NOT NULL, `template_uid` TEXT NOT NULL, `channel` TEXT NOT NULL, `audience_type` TEXT NOT NULL, `audience_ref` TEXT, `variables_json` TEXT, `scheduled_at` TEXT, `throttle_per_minute` INTEGER, `status` TEXT NOT NULL, `targeted_count` INTEGER NOT NULL, `sent_count` INTEGER NOT NULL, `delivered_count` INTEGER NOT NULL, `failed_count` INTEGER NOT NULL, `skipped_count` INTEGER NOT NULL, `started_at` TEXT, `completed_at` TEXT, `synced` INTEGER NOT NULL, `active` INTEGER NOT NULL, `updated_at` TEXT, PRIMARY KEY(`uid`))")
        // comm_logs
        connection.execSQL("CREATE TABLE IF NOT EXISTS `comm_logs` (`uid` TEXT NOT NULL, `request_uid` TEXT, `customer_uid` TEXT, `channel` TEXT NOT NULL, `recipient_address` TEXT NOT NULL, `category` TEXT NOT NULL, `status` TEXT NOT NULL, `skip_reason` TEXT, `provider_message_id` TEXT, `error_message` TEXT, `sent_at` TEXT, `delivered_at` TEXT, `created_at` TEXT, `active` INTEGER NOT NULL, `updated_at` TEXT, PRIMARY KEY(`uid`))")
        // comm_preferences
        connection.execSQL("CREATE TABLE IF NOT EXISTS `comm_preferences` (`uid` TEXT NOT NULL, `customer_uid` TEXT NOT NULL, `channel` TEXT NOT NULL, `category` TEXT NOT NULL, `opted_in` INTEGER NOT NULL, `source` TEXT, `synced` INTEGER NOT NULL, `active` INTEGER NOT NULL, `updated_at` TEXT, PRIMARY KEY(`uid`))")
        // comm_schedules
        connection.execSQL("CREATE TABLE IF NOT EXISTS `comm_schedules` (`uid` TEXT NOT NULL, `name` TEXT NOT NULL, `template_uid` TEXT NOT NULL, `channels` TEXT NOT NULL, `audience_type` TEXT NOT NULL, `audience_ref` TEXT, `variables_json` TEXT, `frequency` TEXT NOT NULL, `interval_count` INTEGER NOT NULL, `day_of_week` INTEGER, `day_of_month` INTEGER, `time_of_day` TEXT NOT NULL, `start_date` TEXT, `end_date` TEXT, `paused` INTEGER NOT NULL, `next_run_at` TEXT, `last_run_at` TEXT, `synced` INTEGER NOT NULL, `active` INTEGER NOT NULL, `updated_at` TEXT, PRIMARY KEY(`uid`))")
        // comm_templates
        connection.execSQL("CREATE TABLE IF NOT EXISTS `comm_templates` (`uid` TEXT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `default_locale` TEXT, `description` TEXT, `base_version` INTEGER NOT NULL, `variants_json` TEXT NOT NULL, `synced` INTEGER NOT NULL, `active` INTEGER NOT NULL, `updated_at` TEXT, PRIMARY KEY(`uid`))")
    }
}
