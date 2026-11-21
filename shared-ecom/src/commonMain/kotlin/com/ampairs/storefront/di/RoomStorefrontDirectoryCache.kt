package com.ampairs.storefront.di

import com.ampairs.common.di.AppScope
import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.data.db.dao.StorefrontDirectoryDao
import com.ampairs.ecom.data.db.entity.StorefrontDirectoryEntity
import com.ampairs.ecom.data.repository.StorefrontDirectoryCache
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock

/**
 * Room-backed [StorefrontDirectoryCache] for the storefront apps (clientApp / marketplaceApp),
 * over the AppScope `storefront_directory` table in `StorefrontAppDatabase`. Bound only on the
 * `:shared-ecom` classpath — the main business app binds a no-op instead (it never shows the
 * directory), which keeps this table out of the main app's database.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RoomStorefrontDirectoryCache(
    private val dao: StorefrontDirectoryDao,
) : StorefrontDirectoryCache {

    override suspend fun replace(stores: List<Storefront>) {
        val now = Clock.System.now().toEpochMilliseconds()
        val rows = stores
            .filter { it.slug.isNotBlank() }
            .mapIndexed { index, s -> s.toDirectoryEntity(index, now) }
        dao.clear()
        dao.insertAll(rows)
    }

    override suspend fun query(query: String?): List<Storefront> {
        val rows = if (query.isNullOrBlank()) dao.all() else dao.search(query.trim())
        return rows.map { it.toStorefront() }
    }

    private fun Storefront.toDirectoryEntity(position: Int, cachedAt: Long) = StorefrontDirectoryEntity(
        slug = slug,
        uid = uid,
        name = name,
        description = description,
        logo_url = logoUrl,
        banner_url = bannerUrl,
        status = status,
        access_mode = accessMode,
        brand_color_argb = brandColorArgb,
        position = position,
        cached_at = cachedAt,
    )

    private fun StorefrontDirectoryEntity.toStorefront() = Storefront(
        uid = uid,
        slug = slug,
        name = name,
        description = description,
        logoUrl = logo_url,
        bannerUrl = banner_url,
        status = status,
        brandColorArgb = brand_color_argb,
        accessMode = access_mode,
    )
}
