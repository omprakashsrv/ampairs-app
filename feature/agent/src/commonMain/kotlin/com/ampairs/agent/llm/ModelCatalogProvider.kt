package com.ampairs.agent.llm

import com.ampairs.agent.data.api.ModelCatalogApi
import com.ampairs.agent.data.api.toDescriptor
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Source of truth for the on-device model catalog. The catalog is **server-seeded** (pulled from
 * `GET /api/workspace/v1/llm-models/catalog`, like the module catalog) — the app never hardcodes
 * model URLs. [ModelCatalog] is kept only as a bundled fallback for first-launch/offline so model
 * selection still has metadata before the first successful pull.
 *
 * Cached in-memory for the process lifetime; [all] lazily refreshes on first use. App-scoped
 * singleton so the cache survives across screens (and workspace switches reuse the same cache —
 * the catalog is global, not per-workspace).
 */
@Inject
@SingleIn(AppScope::class)
class ModelCatalogProvider(
    private val api: ModelCatalogApi,
) {
    private val mutex = Mutex()

    @Volatile
    private var cached: List<ModelDescriptor>? = null

    /** Pull the catalog from the server and replace the in-memory cache. */
    suspend fun refresh(): Result<Int> {
        val response = runCatching { api.catalog() }.getOrElse { return Result.failure(it) }
        val data = response.data
        if (data != null && response.error == null) {
            val models = data.map { it.toDescriptor() }
            mutex.withLock { cached = models }
            return Result.success(models.size)
        }
        return Result.failure(IllegalStateException("Failed to load model catalog"))
    }

    /** Server catalog once pulled (best-effort lazy refresh); the bundled defaults until then. */
    suspend fun all(): List<ModelDescriptor> {
        cached?.let { return it }
        refresh()
        return cached ?: ModelCatalog.all
    }

    suspend fun byId(id: String): ModelDescriptor? = all().firstOrNull { it.id == id }

    suspend fun byRole(role: ModelRole): List<ModelDescriptor> = all().filter { it.role == role }
}
