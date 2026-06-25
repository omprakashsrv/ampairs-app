package com.ampairs.agent.llm

import com.ampairs.agent.data.api.ModelCatalogApi
import com.ampairs.agent.data.api.toDescriptor
import com.ampairs.agent.data.api.toEntity
import com.ampairs.agent.data.api.toResponse
import com.ampairs.agent.data.db.dao.AiModelDao
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Source of truth for the on-device model catalog. The catalog is **server-driven** (pulled from
 * the backend manifest `GET /api/agent/v1/models`) — the app never hardcodes model files. The bundled
 * [ModelCatalog] is only the very-first-launch fallback (before any successful pull).
 *
 * Each successful pull is **persisted** to the app-scoped [AgentCatalogDatabase] (via [AiModelDao]) so
 * the exact server descriptors (id, file name, RAM gate, proxy URL) are available offline. A model is
 * downloaded under its *server* id/file name, so when the backend is unreachable the app reuses the
 * persisted server catalog to recognize, select, and load it; the bundled catalog uses different
 * ids/file names and would make a downloaded model look absent or unselectable. The catalog DB is
 * app-scoped (global), matching the global model-file directory — the same catalog applies to every
 * workspace, so a model downloaded in one workspace is recognized offline in all of them.
 *
 * Cached in-memory for the process lifetime; [all] lazily refreshes on first use. App-scoped
 * singleton so the cache survives across screens (and workspace switches reuse the same cache —
 * the catalog is global, not per-workspace).
 */
@Inject
@SingleIn(AppScope::class)
class ModelCatalogProvider(
    private val api: ModelCatalogApi,
    private val aiModelDao: AiModelDao,
) {
    private val mutex = Mutex()

    @Volatile
    private var cached: List<ModelDescriptor>? = null

    /** Pull the catalog from the server, replace the in-memory cache, and persist it for offline use. */
    suspend fun refresh(): Result<Int> {
        val response = runCatching { api.catalog() }.getOrElse { return Result.failure(it) }
        val data = response.data
        if (data != null && response.error == null) {
            val models = data.map { it.toDescriptor() }
            mutex.withLock { cached = models }
            // Persist the manifest so already-downloaded server models stay selectable + loadable
            // when offline (the bundled fallback uses different ids/file names — see class KDoc).
            runCatching { aiModelDao.replaceAll(data.map { it.toEntity() }) }
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

    /** Map the persisted catalog rows into descriptors, or null if none stored. */
    private suspend fun loadPersisted(): List<ModelDescriptor>? =
        runCatching { aiModelDao.getAll() }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.toResponse().toDescriptor() }

    suspend fun byId(id: String): ModelDescriptor? = all().firstOrNull { it.id == id }

    suspend fun byRole(role: ModelRole): List<ModelDescriptor> = all().filter { it.role == role }
}
