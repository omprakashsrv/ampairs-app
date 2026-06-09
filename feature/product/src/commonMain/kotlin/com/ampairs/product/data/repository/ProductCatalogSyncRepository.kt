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
        count += pullGroups()
        count += pullCategories()
        count += pullBrands()
        count += pullSubCategories()
        count
    }

    private suspend fun pullGroups(): Int {
        var page = 0
        var total = 0
        var hasNext: Boolean
        do {
            val pageResp = api.getGroupsSync(lastSync = null, page = page, size = 100).getOrThrow()
            val batch = pageResp.content
            if (batch.isNotEmpty()) groupDao.insertAll(batch.asGroupDatabaseEntity())
            total += batch.size
            hasNext = pageResp.hasNext
            page++
        } while (hasNext && total < 10000)
        return total
    }

    private suspend fun pullCategories(): Int {
        var page = 0
        var total = 0
        var hasNext: Boolean
        do {
            val pageResp = api.getCategoriesSync(lastSync = null, page = page, size = 100).getOrThrow()
            val batch = pageResp.content
            if (batch.isNotEmpty()) categoryDao.insertAll(batch.asCategoryDatabaseEntity())
            total += batch.size
            hasNext = pageResp.hasNext
            page++
        } while (hasNext && total < 10000)
        return total
    }

    private suspend fun pullBrands(): Int {
        var page = 0
        var total = 0
        var hasNext: Boolean
        do {
            val pageResp = api.getBrandsSync(lastSync = null, page = page, size = 100).getOrThrow()
            val batch = pageResp.content
            if (batch.isNotEmpty()) brandDao.insertAll(batch.asBrandDatabaseEntity())
            total += batch.size
            hasNext = pageResp.hasNext
            page++
        } while (hasNext && total < 10000)
        return total
    }

    private suspend fun pullSubCategories(): Int {
        var page = 0
        var total = 0
        var hasNext: Boolean
        do {
            val pageResp = api.getSubCategoriesSync(lastSync = null, page = page, size = 100).getOrThrow()
            val batch = pageResp.content
            if (batch.isNotEmpty()) subCategoryDao.insertAll(batch.asSubCategoryDatabaseEntity())
            total += batch.size
            hasNext = pageResp.hasNext
            page++
        } while (hasNext && total < 10000)
        return total
    }

    private suspend fun pushGroups(): Int {
        val unsynced = groupDao.unSyncedGroups()
        if (unsynced.isEmpty()) return 0
        return api.updateGroups(unsynced.asGroupApiModel()).fold(
            onSuccess = { unsynced.forEach { groupDao.markAsSynced(it.id) }; unsynced.size },
            onFailure = { throw it },
        )
    }

    private suspend fun pushCategories(): Int {
        val unsynced = categoryDao.unSyncedCategories()
        if (unsynced.isEmpty()) return 0
        return api.updateCategories(unsynced.asCategoryApiModel()).fold(
            onSuccess = { unsynced.forEach { categoryDao.markAsSynced(it.id) }; unsynced.size },
            onFailure = { throw it },
        )
    }

    private suspend fun pushBrands(): Int {
        val unsynced = brandDao.unSyncedBrands()
        if (unsynced.isEmpty()) return 0
        return api.updateBrands(unsynced.asBrandApiModel()).fold(
            onSuccess = { unsynced.forEach { brandDao.markAsSynced(it.id) }; unsynced.size },
            onFailure = { throw it },
        )
    }

    private suspend fun pushSubCategories(): Int {
        val unsynced = subCategoryDao.unSyncedSubCategories()
        if (unsynced.isEmpty()) return 0
        return api.updateSubCategories(unsynced.asSubCategoryApiModel()).fold(
            onSuccess = { unsynced.forEach { subCategoryDao.markAsSynced(it.id) }; unsynced.size },
            onFailure = { throw it },
        )
    }
}
