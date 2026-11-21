package com.ampairs.connector.domain

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.workspace.WorkspaceConfig
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Offline mirror of the backend connector metadata (spec 013, US5). Persists the workspace's
 * installation list and per-installation config as JSON blobs in the shared DataStore so the client
 * connector config/status survives offline and across restarts. Keyed by the active workspace.
 */
@Inject
class ConnectorMirrorStore(
    private val prefs: AppPreferencesDataStore,
    private val config: WorkspaceConfig,
) {
    suspend fun cacheInstallations(installations: List<ConnectorInstallationDto>) {
        runCatching {
            prefs.setConnectorInstallationsJson(
                config.workspaceSlug,
                json.encodeToString(ListSerializer(ConnectorInstallationDto.serializer()), installations),
            )
        }
    }

    suspend fun cachedInstallations(): List<ConnectorInstallationDto> {
        val raw = prefs.getConnectorInstallationsJson(config.workspaceSlug).first() ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ConnectorInstallationDto.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun cacheConfig(installationUid: String, configDto: ConnectorConfigDto) {
        runCatching {
            prefs.setConnectorConfigJson(
                installationUid,
                json.encodeToString(ConnectorConfigDto.serializer(), configDto),
            )
        }
    }

    suspend fun cachedConfig(installationUid: String): ConnectorConfigDto? {
        val raw = prefs.getConnectorConfigJson(installationUid).first() ?: return null
        return runCatching { json.decodeFromString(ConnectorConfigDto.serializer(), raw) }.getOrNull()
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
