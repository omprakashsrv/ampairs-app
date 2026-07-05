package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.data.api.EcomApi
import dev.zacsweers.metro.Inject

/**
 * Cross-storefront directory used by the common (multi-store) app to list every published store the
 * customer can pick from. This is a **non-sync, UI-invoked** read (an allowed exception to the
 * repository-is-local-only rule — see `/offline-sync`), so it may hold the [EcomApi] directly.
 *
 * AppScope (unscoped `@Inject`): the directory exists before any storefront/workspace graph is
 * activated, so it must not depend on workspace-scoped state.
 */
@Inject
class StorefrontDirectoryRepository(
    private val api: EcomApi,
) {
    /** One page of published storefronts. `query` filters by name/slug when non-blank. */
    suspend fun listStorefronts(query: String?, page: Int = 0, size: Int = 20): Result<List<Storefront>> =
        api.listStorefronts(q = query, page = page, size = size).map { it.content }
}
