package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.CatalogMeta
import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.api.model.TaxonomyType
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.data.db.dao.StorefrontDao
import com.ampairs.ecom.data.db.dao.TaxonomyImageDao
import com.ampairs.ecom.data.db.entity.StorefrontEntity
import com.ampairs.ecom.data.db.entity.TaxonomyImageEntity
import com.ampairs.ecom.domain.EcomLogger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

/**
 * Bootstraps storefront branding + taxonomy and exposes them reactively from Room.
 */
@Inject
class StorefrontRepository(
    private val api: EcomApi,
    private val storefrontDao: StorefrontDao,
    private val taxonomyDao: TaxonomyImageDao,
) {
    fun observeStorefront(slug: String): Flow<StorefrontEntity?> = storefrontDao.observeBySlug(slug)

    fun observeTaxonomy(storefrontId: String): Flow<List<TaxonomyImageEntity>> =
        taxonomyDao.observeForStorefront(storefrontId)

    /** Fetch + cache the storefront. Returns the domain model so the caller can resolve the gate. */
    suspend fun bootstrap(slug: String): Result<Storefront> {
        val result = api.getStorefront(slug)
        result.onSuccess { storefront ->
            storefrontDao.upsert(storefront.toEntity(Clock.System.now().toEpochMilliseconds()))
        }.onFailure { EcomLogger.w("Storefront", "bootstrap failed for $slug", it) }
        return result
    }

    /** Refresh category/subcategory/brand tiles for a storefront. */
    suspend fun refreshCatalogMeta(slug: String, storefrontId: String): Result<CatalogMeta> {
        val result = api.getCatalogMeta(slug)
        result.onSuccess { meta ->
            taxonomyDao.upsertAll(meta.toTaxonomyRows(storefrontId))
        }.onFailure { EcomLogger.w("Storefront", "catalog-meta failed for $slug", it) }
        return result
    }

    private fun CatalogMeta.toTaxonomyRows(storefrontId: String): List<TaxonomyImageEntity> {
        val rows = mutableListOf<TaxonomyImageEntity>()
        categories.forEach { cat ->
            rows += TaxonomyImageEntity(
                uid = "$storefrontId:CATEGORY:${cat.name}",
                storefront_id = storefrontId,
                type = TaxonomyType.CATEGORY.name,
                name = cat.name,
                image_url = cat.imageUrl,
                product_count = cat.productCount,
                sort_order = cat.sortOrder,
            )
            cat.subcategories.forEach { sub ->
                rows += TaxonomyImageEntity(
                    uid = "$storefrontId:SUBCATEGORY:${cat.name}:${sub.name}",
                    storefront_id = storefrontId,
                    type = TaxonomyType.SUBCATEGORY.name,
                    name = sub.name,
                    parent_name = cat.name,
                    image_url = sub.imageUrl,
                    product_count = sub.productCount,
                    sort_order = sub.sortOrder,
                )
            }
        }
        brands.forEach { brand ->
            rows += TaxonomyImageEntity(
                uid = "$storefrontId:BRAND:${brand.name}",
                storefront_id = storefrontId,
                type = TaxonomyType.BRAND.name,
                name = brand.name,
                image_url = brand.imageUrl,
                product_count = brand.productCount,
                sort_order = brand.sortOrder,
            )
        }
        return rows
    }
}
