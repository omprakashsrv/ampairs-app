package com.ampairs.product.catalog

import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.SubCategoryDao
import dev.zacsweers.metro.Inject

@Inject
class ProductCatalogRepository(
    private val brandDao: BrandDao,
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao,
    private val groupDao: GroupDao,
) {
    suspend fun getBrands(): List<CatalogItem> =
        brandDao.getActiveBrands().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getCategories(): List<CatalogItem> =
        categoryDao.getActiveCategories().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getSubCategories(): List<CatalogItem> =
        subCategoryDao.getActiveSubCategories().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getGroups(): List<CatalogItem> =
        groupDao.getActiveGroups().map { CatalogItem(it.id, it.name, it.active == 1) }
}

data class CatalogItem(val id: String, val name: String, val active: Boolean)
