package com.ampairs.product.catalog

import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FileRepository
import com.ampairs.file.api.FileUploadStatus
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.SubCategoryEntity
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for the product catalog (brands/categories/sub-categories/groups).
 * The catalog API is owned by the sync layer ([com.ampairs.product.data.repository.ProductCatalogSyncRepository]) —
 * writes here persist to Room as unsynced and mark PRODUCT_CATALOG as PENDING_PUSH so
 * CentralSyncService runs the automatic bulk push.
 */
@OptIn(ExperimentalTime::class)
@Inject
class ProductCatalogRepository(
    private val brandDao: BrandDao,
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao,
    private val groupDao: GroupDao,
    private val fileRepository: FileRepository,
    private val syncStateDao: SyncStateDao,
) {

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.PRODUCT_CATALOG, Clock.System.now().toEpochMilliseconds())
    }
    suspend fun getBrands(): List<CatalogItem> =
        brandDao.getActiveBrands().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getCategories(): List<CatalogItem> =
        categoryDao.getActiveCategories().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getSubCategories(): List<CatalogItem> =
        subCategoryDao.getActiveSubCategories().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getGroups(): List<CatalogItem> =
        groupDao.getActiveGroups().map { CatalogItem(it.id, it.name, it.active == 1) }

    fun observeAllItems(type: ProductCatalogType): Flow<List<CatalogItem>> = when (type) {
        ProductCatalogType.BRANDS -> brandDao.observeBrands().flatMapLatest { brands ->
            if (brands.isEmpty()) flowOf(emptyList())
            else combine(brands.map { brand ->
                fileRepository.observePrimaryFile(FileEntityType.BRAND, brand.id)
                    .map { file -> CatalogItem(brand.id, brand.name, brand.active == 1, file.effectiveUrl()) }
            }) { it.toList() }
        }
        ProductCatalogType.CATEGORIES -> categoryDao.observeCategories().flatMapLatest { categories ->
            if (categories.isEmpty()) flowOf(emptyList())
            else combine(categories.map { category ->
                fileRepository.observePrimaryFile(FileEntityType.CATEGORY, category.id)
                    .map { file -> CatalogItem(category.id, category.name, category.active == 1, file.effectiveUrl()) }
            }) { it.toList() }
        }
        ProductCatalogType.SUB_CATEGORIES -> subCategoryDao.observeSubCategories().flatMapLatest { subCategories ->
            if (subCategories.isEmpty()) flowOf(emptyList())
            else combine(subCategories.map { sub ->
                fileRepository.observePrimaryFile(FileEntityType.SUB_CATEGORY, sub.id)
                    .map { file -> CatalogItem(sub.id, sub.name, sub.active == 1, file.effectiveUrl()) }
            }) { it.toList() }
        }
        ProductCatalogType.GROUPS -> groupDao.observeGroups().flatMapLatest { groups ->
            if (groups.isEmpty()) flowOf(emptyList())
            else combine(groups.map { group ->
                fileRepository.observePrimaryFile(FileEntityType.GROUP, group.id)
                    .map { file -> CatalogItem(group.id, group.name, group.active == 1, file.effectiveUrl()) }
            }) { it.toList() }
        }
    }

    suspend fun getAllItems(type: ProductCatalogType): List<CatalogItem> = when (type) {
        ProductCatalogType.BRANDS -> brandDao.getBrands().map { brand ->
            val file = fileRepository.observePrimaryFile(FileEntityType.BRAND, brand.id).first()
            CatalogItem(brand.id, brand.name, brand.active == 1, file.effectiveUrl())
        }
        ProductCatalogType.CATEGORIES -> categoryDao.getCategories().map { category ->
            val file = fileRepository.observePrimaryFile(FileEntityType.CATEGORY, category.id).first()
            CatalogItem(category.id, category.name, category.active == 1, file.effectiveUrl())
        }
        ProductCatalogType.SUB_CATEGORIES -> subCategoryDao.getSubCategories().map { sub ->
            val file = fileRepository.observePrimaryFile(FileEntityType.SUB_CATEGORY, sub.id).first()
            CatalogItem(sub.id, sub.name, sub.active == 1, file.effectiveUrl())
        }
        ProductCatalogType.GROUPS -> groupDao.getGroups().map { group ->
            val file = fileRepository.observePrimaryFile(FileEntityType.GROUP, group.id).first()
            CatalogItem(group.id, group.name, group.active == 1, file.effectiveUrl())
        }
    }

    suspend fun getItemById(type: ProductCatalogType, id: String): CatalogItem? = when (type) {
        ProductCatalogType.BRANDS -> brandDao.brandById(id)?.let { brand ->
            val file = fileRepository.observePrimaryFile(FileEntityType.BRAND, id).first()
            CatalogItem(brand.id, brand.name, brand.active == 1, file.effectiveUrl())
        }
        ProductCatalogType.CATEGORIES -> categoryDao.categoryById(id)?.let { category ->
            val file = fileRepository.observePrimaryFile(FileEntityType.CATEGORY, id).first()
            CatalogItem(category.id, category.name, category.active == 1, file.effectiveUrl())
        }
        ProductCatalogType.SUB_CATEGORIES -> subCategoryDao.subCategoryById(id)?.let { sub ->
            val file = fileRepository.observePrimaryFile(FileEntityType.SUB_CATEGORY, id).first()
            CatalogItem(sub.id, sub.name, sub.active == 1, file.effectiveUrl())
        }
        ProductCatalogType.GROUPS -> groupDao.groupById(id)?.let { group ->
            val file = fileRepository.observePrimaryFile(FileEntityType.GROUP, id).first()
            CatalogItem(group.id, group.name, group.active == 1, file.effectiveUrl())
        }
    }

    suspend fun createItem(type: ProductCatalogType, uid: String, name: String): Result<Unit> = runCatching {
        when (type) {
            ProductCatalogType.BRANDS -> brandDao.insert(BrandEntity(id = uid, name = name, synced = 0))
            ProductCatalogType.CATEGORIES -> categoryDao.insert(CategoryEntity(id = uid, name = name, synced = 0))
            ProductCatalogType.SUB_CATEGORIES -> subCategoryDao.insert(SubCategoryEntity(id = uid, name = name, synced = 0))
            ProductCatalogType.GROUPS -> groupDao.insert(GroupEntity(id = uid, name = name, synced = 0))
        }
        markPending()
    }

    suspend fun updateItem(
        type: ProductCatalogType,
        id: String,
        name: String,
        active: Boolean,
    ): Result<Unit> = runCatching {
        val activeInt = if (active) 1 else 0
        when (type) {
            ProductCatalogType.BRANDS -> {
                val e = brandDao.brandById(id) ?: error("Brand not found")
                brandDao.insert(e.copy(name = name, active = activeInt, synced = 0))
            }
            ProductCatalogType.CATEGORIES -> {
                val e = categoryDao.categoryById(id) ?: error("Category not found")
                categoryDao.insert(e.copy(name = name, active = activeInt, synced = 0))
            }
            ProductCatalogType.SUB_CATEGORIES -> {
                val e = subCategoryDao.subCategoryById(id) ?: error("Sub-category not found")
                subCategoryDao.insert(e.copy(name = name, active = activeInt, synced = 0))
            }
            ProductCatalogType.GROUPS -> {
                val e = groupDao.groupById(id) ?: error("Group not found")
                groupDao.insert(e.copy(name = name, active = activeInt, synced = 0))
            }
        }
        markPending()
    }
}

private fun FileItem?.effectiveUrl(): String? {
    this ?: return null
    if (uploadStatus == FileUploadStatus.PENDING || uploadStatus == FileUploadStatus.UPLOADING) {
        localPath?.let { return "file://$it" }
    }
    return imageUrl.takeIf { it.isNotBlank() }
}

data class CatalogItem(
    val id: String,
    val name: String,
    val active: Boolean,
    val imageUrl: String? = null,
)
