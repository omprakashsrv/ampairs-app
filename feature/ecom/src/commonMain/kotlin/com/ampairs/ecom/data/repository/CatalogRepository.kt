package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.ListedProduct
import com.ampairs.ecom.api.model.PageResponse
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.data.db.dao.ListedProductDao
import com.ampairs.ecom.data.db.dao.SyncCursorDao
import com.ampairs.ecom.data.db.entity.ListedProductEntity
import com.ampairs.ecom.data.db.entity.SyncCursorEntity
import com.ampairs.ecom.domain.EcomLogger
import com.ampairs.ecom.domain.EcomSession
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/**
 * Catalog read model + cursor-based incremental sync (contract §3, §9).
 * Catalog is read-only on the client, so [pushPendingToServer] is a no-op.
 */
@Inject
class CatalogRepository(
    private val api: EcomApi,
    private val productDao: ListedProductDao,
    private val cursorDao: SyncCursorDao,
    private val session: EcomSession,
) {
    fun observeVisible(storefrontId: String): Flow<List<ListedProductEntity>> =
        productDao.observeVisible(storefrontId)

    fun observeProduct(productId: String): Flow<ListedProductEntity?> =
        productDao.observeById(productId)

    /** Refresh one product from the server (fresh stock) and cache it. */
    suspend fun refreshProduct(slug: String, productId: String, storefrontId: String): Result<ListedProduct> {
        val result = api.getProduct(slug, productId)
        result.onSuccess { productDao.upsert(it.toEntity(storefrontId)) }
        return result
    }

    fun observeFiltered(
        storefrontId: String,
        category: String? = null,
        brand: String? = null,
        subcategory: String? = null,
    ): Flow<List<ListedProductEntity>> = productDao.observeFiltered(storefrontId, category, brand, subcategory)

    suspend fun search(slug: String, query: String, page: Int = 0, size: Int = 20): Result<PageResponse<ListedProduct>> =
        api.searchProducts(slug, query, page, size)

    /** Always re-fetch live product from server — used for pre-checkout stock validation. */
    suspend fun getLiveProduct(slug: String, productId: String): Result<ListedProduct> =
        api.getProduct(slug, productId)

    /**
     * Incremental catalog sync. Seeds via full paged listing on first run (no cursor),
     * then applies deltas via /products/sync while `has_more`. Returns number of upserts.
     */
    suspend fun pullFromServer(): Result<Int> {
        val slug = session.activeSlug ?: return Result.success(0)
        val storefrontId = session.activeStorefrontId ?: return Result.success(0)
        val existingCursor = cursorDao.forStorefront(storefrontId)?.next_since
        return if (existingCursor == null) seed(slug, storefrontId) else incremental(slug, storefrontId, existingCursor)
    }

    private suspend fun seed(slug: String, storefrontId: String): Result<Int> = runCatching {
        var page = 0
        var upserts = 0
        while (true) {
            val pageResp = api.getProducts(slug, page = page, size = 100).getOrThrow()
            val rows = pageResp.content.map { it.toEntity(storefrontId) }
            if (rows.isNotEmpty()) productDao.upsertAll(rows)
            upserts += rows.size
            if (pageResp.last || pageResp.content.isEmpty()) break
            page++
            if (page > 100) break // loop guard (max ~10k records)
        }
        // Use server-authoritative cursor on next incremental call.
        cursorDao.upsert(SyncCursorEntity(storefrontId, nowIso(), Clock.System.now().toEpochMilliseconds()))
        upserts
    }.onFailure { EcomLogger.w("Catalog", "seed failed", it) }

    private suspend fun incremental(slug: String, storefrontId: String, since: String): Result<Int> = runCatching {
        var page = 0
        var upserts = 0
        var nextSince = since
        while (true) {
            val syncPage = api.syncProducts(slug, since = since, page = page, size = 100).getOrThrow()
            val rows = syncPage.items.map { it.toEntity(storefrontId) }
            if (rows.isNotEmpty()) productDao.upsertAll(rows) // is_visible=0 rows hide via mapper
            upserts += rows.size
            syncPage.nextSince?.let { nextSince = it }
            if (!syncPage.hasMore) break
            page++
            if (page > 100) break
        }
        cursorDao.upsert(SyncCursorEntity(storefrontId, nextSince, Clock.System.now().toEpochMilliseconds()))
        upserts
    }.onFailure { EcomLogger.w("Catalog", "incremental sync failed", it) }

    /** Catalog is server-owned; nothing to push. */
    suspend fun pushPendingToServer(): Result<Int> = Result.success(0)

    private fun nowIso(): String = Clock.System.now().toString()
}
