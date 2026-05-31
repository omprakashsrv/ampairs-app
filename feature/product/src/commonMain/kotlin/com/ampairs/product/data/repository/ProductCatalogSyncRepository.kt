package com.ampairs.product.data.repository

import com.ampairs.product.data.api.ProductCatalogApi
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.domain.asBrandApiModel
import com.ampairs.product.domain.asBrandDatabaseEntity
import com.ampairs.product.domain.asCategoryApiModel
import com.ampairs.product.domain.asCategoryDatabaseEntity
import com.ampairs.product.domain.asGroupApiModel
import com.ampairs.product.domain.asGroupDatabaseEntity
import com.ampairs.product.domain.asSubCategoryApiModel
import com.ampairs.product.domain.asSubCategoryDatabaseEntity
import dev.zacsweers.metro.Inject

@Inject
class ProductCatalogSyncRepository(
    private val api: ProductCatalogApi,
    private val groupDao: GroupDao,
    private val categoryDao: CategoryDao,
    private val brandDao: BrandDao,
    private val subCategoryDao: SubCategoryDao,
) {

    suspend fun pushPendingToServer(): Result<Int> = runCatching {
        var count = 0
        count += pushGroups()
        count += pushCategories()
        count += pushBrands()
        count += pushSubCategories()
        count
    }

    suspend fun pullFromServer(): Result<Int> = runCatching {
        var count = 0

        api.getGroups().getOrNull()?.let { models ->
            groupDao.insertAll(models.asGroupDatabaseEntity())
            count += models.size
        }
        api.getCategories().getOrNull()?.let { models ->
            categoryDao.insertAll(models.asCategoryDatabaseEntity())
            count += models.size
        }
        api.getBrands().getOrNull()?.let { models ->
            brandDao.insertAll(models.asBrandDatabaseEntity())
            count += models.size
        }
        api.getSubCategories().getOrNull()?.let { models ->
            subCategoryDao.insertAll(models.asSubCategoryDatabaseEntity())
            count += models.size
        }

        count
    }

    private suspend fun pushGroups(): Int {
        val unsynced = groupDao.unSyncedGroups()
        var count = 0
        for (entity in unsynced) {
            val model = listOf(entity).asGroupApiModel().first()
            api.updateGroup(entity.id, model)
                .onSuccess { groupDao.markAsSynced(entity.id); count++ }
        }
        return count
    }

    private suspend fun pushCategories(): Int {
        val unsynced = categoryDao.unSyncedCategories()
        var count = 0
        for (entity in unsynced) {
            val model = listOf(entity).asCategoryApiModel().first()
            api.updateCategory(entity.id, model)
                .onSuccess { categoryDao.markAsSynced(entity.id); count++ }
        }
        return count
    }

    private suspend fun pushBrands(): Int {
        val unsynced = brandDao.unSyncedBrands()
        var count = 0
        for (entity in unsynced) {
            val model = listOf(entity).asBrandApiModel().first()
            api.updateBrand(entity.id, model)
                .onSuccess { brandDao.markAsSynced(entity.id); count++ }
        }
        return count
    }

    private suspend fun pushSubCategories(): Int {
        val unsynced = subCategoryDao.unSyncedSubCategories()
        var count = 0
        for (entity in unsynced) {
            val model = listOf(entity).asSubCategoryApiModel().first()
            api.updateSubCategory(entity.id, model)
                .onSuccess { subCategoryDao.markAsSynced(entity.id); count++ }
        }
        return count
    }
}
