package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.Storefront

/**
 * Offline cache for the storefront-directory (multi-store picker) listing. The concrete binding is
 * per-app: the storefront apps (`:shared-ecom`) supply a Room-backed impl over
 * `StorefrontAppDatabase`; the main business app (`:shared`) — which never shows the directory —
 * binds a no-op. Deals in the [Storefront] domain model so the entity/mapping stays out of
 * `:feature:ecom` (whose common code can't see the Android-only `StorefrontAppDatabase`).
 */
interface StorefrontDirectoryCache {
    /** Replace the whole cached listing with the latest unfiltered server page (order preserved). */
    suspend fun replace(stores: List<Storefront>)

    /** Cached stores, filtered client-side by [query] (name/slug) when non-blank; empty if none. */
    suspend fun query(query: String?): List<Storefront>
}
