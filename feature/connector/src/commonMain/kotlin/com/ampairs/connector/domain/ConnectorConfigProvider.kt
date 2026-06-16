package com.ampairs.connector.domain

import com.ampairs.connector.data.api.ConnectorApi
import dev.zacsweers.metro.Inject

/**
 * Reads a workspace's connector configuration from the backend (the new source of truth, replacing
 * client-only DataStore config). Client-side connectors (Tally) use this to discover their
 * installation, connection details (host/port) and field mappings before each sync cycle.
 */
@Inject
class ConnectorConfigProvider(
    private val api: ConnectorApi,
) {
    /** The active installation of [connectorType] for the current workspace, if any. */
    suspend fun installation(connectorType: String = TALLY): ConnectorInstallationDto? =
        api.installations().data?.firstOrNull { it.connectorType.equals(connectorType, ignoreCase = true) }

    suspend fun config(installationUid: String): ConnectorConfigDto? =
        api.config(installationUid).data

    suspend fun mappings(installationUid: String): List<FieldMappingDto> =
        api.mappings(installationUid).data ?: emptyList()

    suspend fun host(installationUid: String): String? =
        config(installationUid)?.nonSecretValues?.get("host")?.takeIf { it.isNotBlank() }

    suspend fun port(installationUid: String): Int? =
        config(installationUid)?.nonSecretValues?.get("port")?.trim()?.toIntOrNull()

    companion object {
        const val TALLY = "tally"
    }
}
