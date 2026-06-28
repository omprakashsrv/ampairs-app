package com.ampairs.communication.data.api

import com.ampairs.communication.data.db.CommBindingEntity
import com.ampairs.communication.data.db.CommCampaignEntity
import com.ampairs.communication.data.db.CommLogEntity
import com.ampairs.communication.data.db.CommPreferenceEntity
import com.ampairs.communication.data.db.CommScheduleEntity
import com.ampairs.communication.data.db.CommTemplateEntity
import com.ampairs.communication.data.db.communicationJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Templates (aggregate) -----------------------------------------------------------------------

@Serializable
data class TemplateVariantDto(
    val uid: String,
    val channel: String,
    val locale: String? = null,
    val subject: String? = null,
    @SerialName("html_body") val htmlBody: String? = null,
    @SerialName("text_body") val textBody: String? = null,
    @SerialName("provider_template_id") val providerTemplateId: String? = null,
    @SerialName("provider_params_json") val providerParamsJson: String? = null,
    val active: Boolean = true,
)

@Serializable
data class TemplateAggregateDto(
    val uid: String,
    val code: String,
    val name: String,
    val category: String,
    @SerialName("default_locale") val defaultLocale: String? = null,
    val description: String? = null,
    @SerialName("base_version") val baseVersion: Long = 0,
    val variants: List<TemplateVariantDto> = emptyList(),
    val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun CommTemplateEntity.toDto(): TemplateAggregateDto = TemplateAggregateDto(
    uid = uid, code = code, name = name, category = category, defaultLocale = defaultLocale,
    description = description, baseVersion = baseVersion,
    variants = communicationJson.decodeFromString(variantsJson),
    active = active, updatedAt = updatedAt,
)

fun TemplateAggregateDto.toEntity(synced: Boolean): CommTemplateEntity = CommTemplateEntity(
    uid = uid, code = code, name = name, category = category, defaultLocale = defaultLocale,
    description = description, baseVersion = baseVersion,
    variantsJson = communicationJson.encodeToString(variants),
    synced = synced, active = active, updatedAt = updatedAt,
)

// --- Bindings ------------------------------------------------------------------------------------

@Serializable
data class BindingDto(
    val uid: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("template_uid") val templateUid: String,
    val channels: List<String> = emptyList(),
    val enabled: Boolean = true,
    val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun CommBindingEntity.toDto(): BindingDto = BindingDto(
    uid = uid, eventType = eventType, templateUid = templateUid,
    channels = channels.split(",").filter { it.isNotBlank() },
    enabled = enabled, active = active, updatedAt = updatedAt,
)

fun BindingDto.toEntity(synced: Boolean): CommBindingEntity = CommBindingEntity(
    uid = uid, eventType = eventType, templateUid = templateUid,
    channels = channels.joinToString(","),
    enabled = enabled, synced = synced, active = active, updatedAt = updatedAt,
)

// --- Schedules -----------------------------------------------------------------------------------

@Serializable
data class ScheduleDto(
    val uid: String,
    val name: String,
    @SerialName("template_uid") val templateUid: String,
    val channels: List<String> = emptyList(),
    @SerialName("audience_type") val audienceType: String,
    @SerialName("audience_ref") val audienceRef: String? = null,
    @SerialName("variables_json") val variablesJson: String? = null,
    val frequency: String,
    val interval: Int = 1,
    @SerialName("day_of_week") val dayOfWeek: Int? = null,
    @SerialName("day_of_month") val dayOfMonth: Int? = null,
    @SerialName("time_of_day") val timeOfDay: String,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val paused: Boolean = false,
    @SerialName("next_run_at") val nextRunAt: String? = null,
    @SerialName("last_run_at") val lastRunAt: String? = null,
    val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun CommScheduleEntity.toDto(): ScheduleDto = ScheduleDto(
    uid = uid, name = name, templateUid = templateUid,
    channels = channels.split(",").filter { it.isNotBlank() },
    audienceType = audienceType, audienceRef = audienceRef, variablesJson = variablesJson,
    frequency = frequency, interval = interval, dayOfWeek = dayOfWeek, dayOfMonth = dayOfMonth,
    timeOfDay = timeOfDay, startDate = startDate, endDate = endDate, paused = paused,
    active = active, updatedAt = updatedAt,
)

fun ScheduleDto.toEntity(synced: Boolean): CommScheduleEntity = CommScheduleEntity(
    uid = uid, name = name, templateUid = templateUid,
    channels = channels.joinToString(","),
    audienceType = audienceType, audienceRef = audienceRef, variablesJson = variablesJson,
    frequency = frequency, interval = interval, dayOfWeek = dayOfWeek, dayOfMonth = dayOfMonth,
    timeOfDay = timeOfDay, startDate = startDate, endDate = endDate, paused = paused,
    nextRunAt = nextRunAt, lastRunAt = lastRunAt,
    synced = synced, active = active, updatedAt = updatedAt,
)

// --- Campaigns -----------------------------------------------------------------------------------

@Serializable
data class CampaignDto(
    val uid: String,
    val name: String,
    @SerialName("template_uid") val templateUid: String,
    val channel: String,
    @SerialName("audience_type") val audienceType: String,
    @SerialName("audience_ref") val audienceRef: String? = null,
    @SerialName("variables_json") val variablesJson: String? = null,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("throttle_per_minute") val throttlePerMinute: Int? = null,
    val status: String = "DRAFT",
    @SerialName("targeted_count") val targetedCount: Int = 0,
    @SerialName("sent_count") val sentCount: Int = 0,
    @SerialName("delivered_count") val deliveredCount: Int = 0,
    @SerialName("failed_count") val failedCount: Int = 0,
    @SerialName("skipped_count") val skippedCount: Int = 0,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun CommCampaignEntity.toDto(): CampaignDto = CampaignDto(
    uid = uid, name = name, templateUid = templateUid, channel = channel,
    audienceType = audienceType, audienceRef = audienceRef, variablesJson = variablesJson,
    scheduledAt = scheduledAt, throttlePerMinute = throttlePerMinute, status = status,
    targetedCount = targetedCount, sentCount = sentCount, deliveredCount = deliveredCount,
    failedCount = failedCount, skippedCount = skippedCount, startedAt = startedAt,
    completedAt = completedAt, active = active, updatedAt = updatedAt,
)

fun CampaignDto.toEntity(synced: Boolean): CommCampaignEntity = CommCampaignEntity(
    uid = uid, name = name, templateUid = templateUid, channel = channel,
    audienceType = audienceType, audienceRef = audienceRef, variablesJson = variablesJson,
    scheduledAt = scheduledAt, throttlePerMinute = throttlePerMinute, status = status,
    targetedCount = targetedCount, sentCount = sentCount, deliveredCount = deliveredCount,
    failedCount = failedCount, skippedCount = skippedCount, startedAt = startedAt,
    completedAt = completedAt, synced = synced, active = active, updatedAt = updatedAt,
)

// --- Preferences ---------------------------------------------------------------------------------

@Serializable
data class PreferenceDto(
    val uid: String,
    @SerialName("customer_uid") val customerUid: String,
    val channel: String,
    val category: String,
    @SerialName("opted_in") val optedIn: Boolean,
    val source: String? = null,
    val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun CommPreferenceEntity.toDto(): PreferenceDto = PreferenceDto(
    uid = uid, customerUid = customerUid, channel = channel, category = category,
    optedIn = optedIn, source = source, active = active, updatedAt = updatedAt,
)

fun PreferenceDto.toEntity(synced: Boolean): CommPreferenceEntity = CommPreferenceEntity(
    uid = uid, customerUid = customerUid, channel = channel, category = category,
    optedIn = optedIn, source = source, synced = synced, active = active, updatedAt = updatedAt,
)

// --- Logs (pull-only) ----------------------------------------------------------------------------

@Serializable
data class CommunicationLogDto(
    val uid: String,
    @SerialName("request_uid") val requestUid: String? = null,
    @SerialName("customer_uid") val customerUid: String? = null,
    val channel: String,
    @SerialName("recipient_address") val recipientAddress: String = "",
    val category: String,
    val status: String,
    @SerialName("skip_reason") val skipReason: String? = null,
    @SerialName("provider_message_id") val providerMessageId: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val active: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun CommunicationLogDto.toEntity(): CommLogEntity = CommLogEntity(
    uid = uid, requestUid = requestUid, customerUid = customerUid, channel = channel,
    recipientAddress = recipientAddress, category = category, status = status,
    skipReason = skipReason, providerMessageId = providerMessageId, errorMessage = errorMessage,
    sentAt = sentAt, deliveredAt = deliveredAt, createdAt = createdAt, active = active,
    updatedAt = updatedAt,
)

// --- Credentials (non-sync; secrets write-only) --------------------------------------------------

@Serializable
data class CredentialRequest(
    val channel: String,
    val provider: String,
    @SerialName("sender_ref") val senderRef: String,
    @SerialName("display_name") val displayName: String? = null,
    val secret: String,
    @SerialName("config_json") val configJson: String? = null,
    @SerialName("allow_platform_fallback") val allowPlatformFallback: Boolean = false,
)

@Serializable
data class CredentialResponse(
    val uid: String,
    val channel: String,
    val provider: String,
    @SerialName("sender_ref") val senderRef: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("secret_last4") val secretLast4: String? = null,
    @SerialName("config_json") val configJson: String? = null,
    @SerialName("allow_platform_fallback") val allowPlatformFallback: Boolean = false,
    val status: String = "UNVERIFIED",
    @SerialName("last_validated_at") val lastValidatedAt: String? = null,
    val active: Boolean = true,
)

// --- Usage report (non-sync) ---------------------------------------------------------------------

@Serializable
data class UsageRowDto(
    val channel: String,
    @SerialName("credential_uid") val credentialUid: String? = null,
    @SerialName("provider_account_ref") val providerAccountRef: String? = null,
    @SerialName("billing_mode") val billingMode: String,
    @SerialName("message_count") val messageCount: Long = 0,
    @SerialName("cost_units") val costUnits: Long = 0,
    @SerialName("cost_category") val costCategory: String? = null,
)

@Serializable
data class UsageReportResponse(
    val rows: List<UsageRowDto> = emptyList(),
    @SerialName("total_messages") val totalMessages: Long = 0,
    @SerialName("total_cost_units") val totalCostUnits: Long = 0,
)

// --- Preview (non-sync) --------------------------------------------------------------------------

@Serializable
data class PreviewRequest(
    val channel: String,
    val locale: String? = null,
    val variables: Map<String, String> = emptyMap(),
)

@Serializable
data class PreviewResponse(
    val subject: String? = null,
    @SerialName("rendered_html") val renderedHtml: String? = null,
    @SerialName("rendered_text") val renderedText: String = "",
    @SerialName("missing_variables") val missingVariables: List<String> = emptyList(),
)
