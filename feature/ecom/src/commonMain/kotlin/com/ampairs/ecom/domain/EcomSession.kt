package com.ampairs.ecom.domain

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the active storefront for the current session. Sync delegates and repositories
 * read [activeSlug] / [activeStorefrontId] to scope their work. Set when a storefront is
 * opened (deep link or last-store), cleared on logout / store switch.
 *
 * App-scoped singleton — the storefront identity is stable for the lifetime of a visit.
 */
@Inject
@SingleIn(AppScope::class)
class EcomSession {
    private val _active = MutableStateFlow<ActiveStorefront?>(null)
    val active: StateFlow<ActiveStorefront?> = _active.asStateFlow()

    val activeSlug: String? get() = _active.value?.slug
    val activeStorefrontId: String? get() = _active.value?.storefrontId

    fun open(slug: String, storefrontId: String) {
        _active.value = ActiveStorefront(slug = slug, storefrontId = storefrontId)
    }

    fun clear() {
        _active.value = null
    }
}

data class ActiveStorefront(
    val slug: String,
    val storefrontId: String,
)
