package com.ampairs.communication.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json

/** Shared JSON for (de)serializing embedded lists (template variants, channel lists). */
internal val communicationJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// --- Message template (aggregate: header + variants bundled in one row) ---------------------------

/**
 * A message template aggregate. The ordered channel/locale [variantsJson] is stored inline (one row
 * per template), mirroring the backend aggregate-grained `/sync` feed. `baseVersion` carries the
 * server's optimistic-concurrency token used on push.
 */
@Entity(tableName = "comm_templates")
data class CommTemplateEntity(
    @PrimaryKey @ColumnInfo("uid") val uid: String,
    @ColumnInfo("code") val code: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("category") val category: String,
    @ColumnInfo("default_locale") val defaultLocale: String? = null,
    @ColumnInfo("description") val description: String? = null,
    @ColumnInfo("base_version") val baseVersion: Long = 0,
    @ColumnInfo("variants_json") val variantsJson: String = "[]",
    @ColumnInfo("synced") val synced: Boolean = false,
    @ColumnInfo("active") val active: Boolean = true,
    @ColumnInfo("updated_at") val updatedAt: String? = null,
)

// --- Event -> template binding -------------------------------------------------------------------

@Entity(tableName = "comm_bindings")
data class CommBindingEntity(
    @PrimaryKey @ColumnInfo("uid") val uid: String,
    @ColumnInfo("event_type") val eventType: String,
    @ColumnInfo("template_uid") val templateUid: String,
    /** Comma-joined channels (e.g. "EMAIL,SMS"). */
    @ColumnInfo("channels") val channels: String,
    @ColumnInfo("enabled") val enabled: Boolean = true,
    @ColumnInfo("synced") val synced: Boolean = false,
    @ColumnInfo("active") val active: Boolean = true,
    @ColumnInfo("updated_at") val updatedAt: String? = null,
)

// --- Recurring schedule --------------------------------------------------------------------------

@Entity(tableName = "comm_schedules")
data class CommScheduleEntity(
    @PrimaryKey @ColumnInfo("uid") val uid: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("template_uid") val templateUid: String,
    @ColumnInfo("channels") val channels: String,
    @ColumnInfo("audience_type") val audienceType: String,
    @ColumnInfo("audience_ref") val audienceRef: String? = null,
    @ColumnInfo("variables_json") val variablesJson: String? = null,
    @ColumnInfo("frequency") val frequency: String,
    @ColumnInfo("interval_count") val interval: Int = 1,
    @ColumnInfo("day_of_week") val dayOfWeek: Int? = null,
    @ColumnInfo("day_of_month") val dayOfMonth: Int? = null,
    @ColumnInfo("time_of_day") val timeOfDay: String,
    @ColumnInfo("start_date") val startDate: String? = null,
    @ColumnInfo("end_date") val endDate: String? = null,
    @ColumnInfo("paused") val paused: Boolean = false,
    // Server-owned (read-only) — kept for display.
    @ColumnInfo("next_run_at") val nextRunAt: String? = null,
    @ColumnInfo("last_run_at") val lastRunAt: String? = null,
    @ColumnInfo("synced") val synced: Boolean = false,
    @ColumnInfo("active") val active: Boolean = true,
    @ColumnInfo("updated_at") val updatedAt: String? = null,
)

// --- Promotional campaign ------------------------------------------------------------------------

@Entity(tableName = "comm_campaigns")
data class CommCampaignEntity(
    @PrimaryKey @ColumnInfo("uid") val uid: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("template_uid") val templateUid: String,
    @ColumnInfo("channel") val channel: String,
    @ColumnInfo("audience_type") val audienceType: String,
    @ColumnInfo("audience_ref") val audienceRef: String? = null,
    @ColumnInfo("variables_json") val variablesJson: String? = null,
    @ColumnInfo("scheduled_at") val scheduledAt: String? = null,
    @ColumnInfo("throttle_per_minute") val throttlePerMinute: Int? = null,
    // Server-owned rollup (read-only).
    @ColumnInfo("status") val status: String = "DRAFT",
    @ColumnInfo("targeted_count") val targetedCount: Int = 0,
    @ColumnInfo("sent_count") val sentCount: Int = 0,
    @ColumnInfo("delivered_count") val deliveredCount: Int = 0,
    @ColumnInfo("failed_count") val failedCount: Int = 0,
    @ColumnInfo("skipped_count") val skippedCount: Int = 0,
    @ColumnInfo("started_at") val startedAt: String? = null,
    @ColumnInfo("completed_at") val completedAt: String? = null,
    @ColumnInfo("synced") val synced: Boolean = false,
    @ColumnInfo("active") val active: Boolean = true,
    @ColumnInfo("updated_at") val updatedAt: String? = null,
)

// --- Consent / preference ------------------------------------------------------------------------

@Entity(tableName = "comm_preferences")
data class CommPreferenceEntity(
    @PrimaryKey @ColumnInfo("uid") val uid: String,
    @ColumnInfo("customer_uid") val customerUid: String,
    @ColumnInfo("channel") val channel: String,
    @ColumnInfo("category") val category: String,
    @ColumnInfo("opted_in") val optedIn: Boolean,
    @ColumnInfo("source") val source: String? = null,
    @ColumnInfo("synced") val synced: Boolean = false,
    @ColumnInfo("active") val active: Boolean = true,
    @ColumnInfo("updated_at") val updatedAt: String? = null,
)

// --- Delivery log (server-authored; pull-only — never pushed) ------------------------------------

@Entity(tableName = "comm_logs")
data class CommLogEntity(
    @PrimaryKey @ColumnInfo("uid") val uid: String,
    @ColumnInfo("request_uid") val requestUid: String? = null,
    @ColumnInfo("customer_uid") val customerUid: String? = null,
    @ColumnInfo("channel") val channel: String,
    @ColumnInfo("recipient_address") val recipientAddress: String,
    @ColumnInfo("category") val category: String,
    @ColumnInfo("status") val status: String,
    @ColumnInfo("skip_reason") val skipReason: String? = null,
    @ColumnInfo("provider_message_id") val providerMessageId: String? = null,
    @ColumnInfo("error_message") val errorMessage: String? = null,
    @ColumnInfo("sent_at") val sentAt: String? = null,
    @ColumnInfo("delivered_at") val deliveredAt: String? = null,
    @ColumnInfo("created_at") val createdAt: String? = null,
    @ColumnInfo("active") val active: Boolean = true,
    @ColumnInfo("updated_at") val updatedAt: String? = null,
)
