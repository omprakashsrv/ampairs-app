package com.ampairs.di

import com.ampairs.common.di.AppScope
import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.data.repository.StorefrontDirectoryCache
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * No-op [StorefrontDirectoryCache] for the main business app. The storefront directory (multi-store
 * picker) is only ever shown by the storefront apps (`:shared-ecom`), so the `StorefrontDirectoryViewModel`
 * that Metro merges into this graph is never navigated to here — but its dependency still has to
 * resolve. The Room-backed cache lives only on the `:shared-ecom` classpath (over
 * `StorefrontAppDatabase`), which the main app doesn't have, so this binding satisfies the graph
 * without adding a directory table to the main app's database.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NoOpStorefrontDirectoryCache : StorefrontDirectoryCache {
    override suspend fun replace(stores: List<Storefront>) = Unit
    override suspend fun query(query: String?): List<Storefront> = emptyList()
}
