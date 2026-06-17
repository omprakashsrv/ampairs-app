package com.ampairs.connector.domain

import com.ampairs.connector.data.api.ConnectorApi
import dev.zacsweers.metro.Inject

/**
 * Reads a workspace's connector configuration. The backend is the source of truth; results are
 * mirrored into [ConnectorMirrorStore] (offline cache) on every successful fetch and served from the
 * cache when the backend is unreachable (spec 013, US5). Client-side connectors (Tally) use this to
 * discover their installation, connection details (host/port) and field mappings before each sync.
 */
@Inject
class ConnectorConfigProvider(
    private val api: ConnectorApi,
    private val mirror: ConnectorMirrorStore,
) {
    /** The active installation of [connectorType] for the current workspace, if any. */
    suspend fun installation(connectorType: String = TALLY): ConnectorInstallationDto? {
        val live = runCatching { api.installations().data }.getOrNull()
        if (live != null) {
            mirror.cacheInstallations(live)
            return live.firstOrNull { it.connectorType.equals(connectorType, ignoreCase = true) }
        }
        // Offline: serve the last cached installation list.
        return mirror.cachedInstallations().firstOrNull { it.connectorType.equals(connectorType, ignoreCase = true) }
    }

    suspend fun config(installationUid: String): ConnectorConfigDto? {
        val live = runCatching { api.config(installationUid).data }.getOrNull()
        if (live != null) {
            mirror.cacheConfig(installationUid, live)
            return live
        }
        return mirror.cachedConfig(installationUid)
    }

    suspend fun mappings(installationUid: String): List<FieldMappingDto> =
        runCatching { api.mappings(installationUid).data }.getOrNull() ?: emptyList()

    suspend fun host(installationUid: String): String? =
        config(installationUid)?.nonSecretValues?.get("host")?.takeIf { it.isNotBlank() }

    suspend fun port(installationUid: String): Int? =
        config(installationUid)?.nonSecretValues?.get("port")?.trim()?.toIntOrNull()

    companion object {
        const val TALLY = "tally"
    }
}
