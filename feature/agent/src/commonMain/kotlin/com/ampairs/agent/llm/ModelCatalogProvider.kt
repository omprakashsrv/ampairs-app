package com.ampairs.agent.llm

import com.ampairs.agent.data.api.AiModelResponse
import com.ampairs.agent.data.api.ModelCatalogApi
import com.ampairs.agent.data.api.toDescriptor
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Source of truth for the on-device model catalog. The catalog is **server-driven** (pulled from
 * the backend manifest `GET /api/agent/v1/models`) — the app never hardcodes model files. The bundled
 * [ModelCatalog] is only the very-first-launch fallback (before any successful pull).
 *
 * Each successful pull is **persisted** (the raw manifest JSON, via [AppPreferencesDataStore]) so the
 * exact server descriptors (id, file name, RAM gate, proxy URL) are available offline. A model is
 * downloaded under its *server* id/file name, so when the backend is unreachable the app reuses the
 * persisted server catalog to recognize, select, and load it; the bundled catalog uses different
 * ids/file names and would make a downloaded model look absent or unselectable.
 *
 * Cached in-memory for the process lifetime; [all] lazily refreshes on first use. App-scoped
 * singleton so the cache survives across screens (and workspace switches reuse the same cache —
 * the catalog is global, not per-workspace).
 */
@Inject
@SingleIn(AppScope::class)
class ModelCatalogProvider(
    private val api: ModelCatalogApi,
    private val appPreferences: AppPreferencesDataStore,
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: List<ModelDescriptor>? = null

    /** Pull the catalog from the server, replace the in-memory cache, and persist it for offline use. */
    suspend fun refresh(): Result<Int> {
        val response = runCatching { api.catalog() }.getOrElse { return Result.failure(it) }
        val data = response.data
        if (data != null && response.error == null) {
            val models = data.map { it.toDescriptor() }
            mutex.withLock { cached = models }
            // Persist the raw manifest so already-downloaded server models stay selectable + loadable
            // when offline (the bundled fallback uses different ids/file names — see class KDoc).
            runCatching {
                appPreferences.setCachedAgentModelCatalog(
                    json.encodeToString(ListSerializer(AiModelResponse.serializer()), data),
                )
            }
            return Result.success(models.size)
        }
        return Result.failure(IllegalStateException("Failed to load model catalog"))
    }

    /**
     * Server catalog once pulled (best-effort lazy refresh). When the pull fails (offline / server
     * unreachable), fall back to the last-known persisted server catalog so downloaded models remain
     * usable; only with no persisted catalog at all do we use the bundled defaults.
     */
    suspend fun all(): List<ModelDescriptor> {
        cached?.let { return it }
        refresh()
        cached?.let { return it }
        loadPersisted()?.let { restored ->
            mutex.withLock { if (cached == null) cached = restored }
            return restored
        }
        return ModelCatalog.all
    }

    /** Deserialize the last persisted manifest into descriptors, or null if absent/unparseable. */
    private suspend fun loadPersisted(): List<ModelDescriptor>? {
        val raw = runCatching { appPreferences.getCachedAgentModelCatalog().first() }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            json.decodeFromString(ListSerializer(AiModelResponse.serializer()), raw)
        }.getOrNull()?.takeIf { it.isNotEmpty() }?.map { it.toDescriptor() }
    }

    suspend fun byId(id: String): ModelDescriptor? = all().firstOrNull { it.id == id }

    suspend fun byRole(role: ModelRole): List<ModelDescriptor> = all().filter { it.role == role }
}
