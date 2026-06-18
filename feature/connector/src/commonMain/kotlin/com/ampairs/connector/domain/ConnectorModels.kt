package com.ampairs.connector.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A connector installed for the current workspace (backend `/connector/v1/installations`). */
@Serializable
data class ConnectorInstallationDto(
    val uid: String = "",
    @SerialName("connector_type") val connectorType: String = "",
    val status: String = "",
    @SerialName("auto_start") val autoStart: Boolean = true,
    @SerialName("schedule_seconds") val scheduleSeconds: Int? = null,
    @SerialName("last_error_message") val lastErrorMessage: String? = null,
)

/** Connection config (secrets masked — only the set keys are listed). */
@Serializable
data class ConnectorConfigDto(
    @SerialName("installation_uid") val installationUid: String = "",
    @SerialName("non_secret_values") val nonSecretValues: Map<String, String> = emptyMap(),
    @SerialName("secret_keys_set") val secretKeysSet: List<String> = emptyList(),
    @SerialName("last_validated_at") val lastValidatedAt: String? = null,
)

/** Write connection config (PUT). `secretValues` is write-only and never echoed back. */
@Serializable
data class ConfigUpdateRequest(
    @SerialName("non_secret_values") val nonSecretValues: Map<String, String> = emptyMap(),
    @SerialName("secret_values") val secretValues: Map<String, String> = emptyMap(),
)

@Serializable
data class FieldMappingRuleDto(
    @SerialName("external_field") val externalField: String = "",
    @SerialName("ampairs_field") val ampairsField: String = "",
    val unmapped: Boolean = false,
    val transform: String? = null,
)

@Serializable
data class FieldMappingDto(
    @SerialName("installation_uid") val installationUid: String = "",
    @SerialName("entity_type") val entityType: String = "",
    val version: Int = 1,
    val rules: List<FieldMappingRuleDto> = emptyList(),
)

/**
 * ONE external record for the connector sparse upsert. [values] is a SPARSE JSON object keyed by
 * Ampairs column name — KEY PRESENCE is the signal: an absent key is left untouched on the server,
 * a present key with null clears it. Build it with only the columns this row provides.
 */
@Serializable
data class SparseUpsertRow(
    @SerialName("ref_id") val refId: String? = null,
    val uid: String? = null,
    val values: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class SparseUpsertResult(
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("ampairs_uid") val ampairsUid: String? = null,
    val outcome: String = "",
    @SerialName("applied_columns") val appliedColumns: List<String> = emptyList(),
    val message: String? = null,
)

@Serializable
data class SyncCheckpointDto(
    @SerialName("installation_uid") val installationUid: String = "",
    @SerialName("entity_type") val entityType: String = "",
    val direction: String = "INBOUND",
    val watermark: String? = null,
    @SerialName("last_synced_at") val lastSyncedAt: String? = null,
)

@Serializable
data class SyncRunDto(
    @SerialName("installation_uid") val installationUid: String = "",
    @SerialName("entity_type") val entityType: String? = null,
    val trigger: String = "MANUAL",
    val status: String = "SUCCESS",
    val processed: Int = 0,
    val created: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    @SerialName("error_detail") val errorDetail: String? = null,
)

@Serializable
data class ConnectionTestRequest(
    val ok: Boolean = false,
    val message: String? = null,
)

@Serializable
data class ConnectionTestResult(
    val ok: Boolean = false,
    val message: String? = null,
    @SerialName("last_validated_at") val lastValidatedAt: String? = null,
)
