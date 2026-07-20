package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.domain.EcomLogger
import dev.zacsweers.metro.Inject

/**
 * One page of the storefront directory plus whether it was served from the offline cache (so the
 * picker can show a subtle "showing saved stores" hint instead of a dead-end error).
 */
data class DirectoryPage(
    val stores: List<Storefront>,
    val fromCache: Boolean,
)

/**
 * Cross-storefront directory used by the common (multi-store) app to list every published store the
 * customer can pick from. This is a **non-sync, UI-invoked** read (an allowed exception to the
 * repository-is-local-only rule — see `/offline-sync`), so it may hold the [EcomApi] directly.
 *
 * Cache-first for offline: a successful *unfiltered* fetch refreshes [StorefrontDirectoryCache];
 * when the network fails, the last cached listing is served (filtered client-side) so the picker
 * still works offline. A network failure with no cache surfaces as `Result.failure`.
 *
 * AppScope (unscoped `@Inject`): the directory exists before any storefront/workspace graph is
 * activated, so it must not depend on workspace-scoped state.
 */
@Inject
class StorefrontDirectoryRepository(
    private val api: EcomApi,
    private val cache: StorefrontDirectoryCache,
) {
    /**
     * One page of published storefronts. `query` filters by name/slug when non-blank. Refreshes
     * from the network first; falls back to the offline cache when the network is unavailable.
     */
    suspend fun listStorefronts(query: String?, page: Int = 0, size: Int = 20): Result<DirectoryPage> =
        api.listStorefronts(q = query, page = page, size = size).map { it.content }.fold(
            onSuccess = { stores ->
                // Only the first unfiltered page is a faithful snapshot of the full directory —
                // never overwrite the cache with a search subset or a later page.
                if (query.isNullOrBlank() && page == 0) {
                    runCatching { cache.replace(stores) }
                        .onFailure { EcomLogger.w("Directory", "cache write failed", it) }
                }
                Result.success(DirectoryPage(stores, fromCache = false))
            },
            onFailure = { e ->
                val cached = runCatching { cache.query(query) }.getOrDefault(emptyList())
                if (cached.isNotEmpty()) {
                    EcomLogger.w("Directory", "network failed — serving ${cached.size} cached stores", e)
                    Result.success(DirectoryPage(cached, fromCache = true))
                } else {
                    Result.failure(e)
                }
            },
        )
}
