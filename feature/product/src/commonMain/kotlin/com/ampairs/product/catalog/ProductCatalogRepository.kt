package com.ampairs.product.catalog

import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.product.data.api.ProductCatalogApi
import com.ampairs.product.db.Constants
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ImageDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ImageEntity
import com.ampairs.product.db.entity.SubCategoryEntity
import dev.zacsweers.metro.Inject

@Inject
class ProductCatalogRepository(
    private val brandDao: BrandDao,
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao,
    private val groupDao: GroupDao,
    private val imageDao: ImageDao,
    private val api: ProductCatalogApi,
) {
    suspend fun getBrands(): List<CatalogItem> =
        brandDao.getActiveBrands().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getCategories(): List<CatalogItem> =
        categoryDao.getActiveCategories().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getSubCategories(): List<CatalogItem> =
        subCategoryDao.getActiveSubCategories().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getGroups(): List<CatalogItem> =
        groupDao.getActiveGroups().map { CatalogItem(it.id, it.name, it.active == 1) }

    suspend fun getAllItems(type: ProductCatalogType): List<CatalogItem> = when (type) {
        ProductCatalogType.BRANDS -> brandDao.getBrands().map {
            CatalogItem(it.brand.id, it.brand.name, it.brand.active == 1, it.image?.imageUrl())
        }
        ProductCatalogType.CATEGORIES -> categoryDao.getCategories().map {
            CatalogItem(it.category.id, it.category.name, it.category.active == 1, it.image?.imageUrl())
        }
        ProductCatalogType.SUB_CATEGORIES -> subCategoryDao.getSubCategories().map {
            CatalogItem(it.subCategory.id, it.subCategory.name, it.subCategory.active == 1, it.image?.imageUrl())
        }
        ProductCatalogType.GROUPS -> groupDao.getGroupModels().map {
            CatalogItem(it.group.id, it.group.name, it.group.active == 1, it.image?.imageUrl())
        }
    }

    suspend fun getItemById(type: ProductCatalogType, id: String): CatalogItem? = when (type) {
        ProductCatalogType.BRANDS -> brandDao.brandModelById(id)?.let {
            CatalogItem(it.brand.id, it.brand.name, it.brand.active == 1, it.image?.imageUrl())
        }
        ProductCatalogType.CATEGORIES -> categoryDao.categoryModelById(id)?.let {
            CatalogItem(it.category.id, it.category.name, it.category.active == 1, it.image?.imageUrl())
        }
        ProductCatalogType.SUB_CATEGORIES -> subCategoryDao.subCategoryModelById(id)?.let {
            CatalogItem(it.subCategory.id, it.subCategory.name, it.subCategory.active == 1, it.image?.imageUrl())
        }
        ProductCatalogType.GROUPS -> groupDao.groupModelById(id)?.let {
            CatalogItem(it.group.id, it.group.name, it.group.active == 1, it.image?.imageUrl())
        }
    }

    suspend fun createItem(type: ProductCatalogType, uid: String, name: String): Result<Unit> = runCatching {
        when (type) {
            ProductCatalogType.BRANDS -> brandDao.insert(BrandEntity(id = uid, name = name, synced = 0))
            ProductCatalogType.CATEGORIES -> categoryDao.insert(CategoryEntity(id = uid, name = name, synced = 0))
            ProductCatalogType.SUB_CATEGORIES -> subCategoryDao.insert(SubCategoryEntity(id = uid, name = name, synced = 0))
            ProductCatalogType.GROUPS -> groupDao.insert(GroupEntity(id = uid, name = name, synced = 0))
        }
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
    }

    // Uploads image to server, persists ImageEntity locally, updates entity's image_id.
    suspend fun uploadItemImage(
        type: ProductCatalogType,
        itemId: String,
        fileName: String,
        contentType: String,
        imageData: ByteArray,
    ): Result<String> = runCatching {
        val imageUid = UidGenerator.generateUid(Constants.PRODUCT_IMAGE_PREFIX)
        val apiImage = api.uploadCatalogItemImage(
            uid = imageUid,
            refUid = itemId,
            fileName = fileName,
            contentType = contentType,
            imageData = imageData,
        ).getOrThrow()

        val imageEntity = ImageEntity(
            id = apiImage.id,
            name = apiImage.name,
            bucket = apiImage.bucket,
            object_key = apiImage.objectKey,
        )
        imageDao.insert(imageEntity)

        when (type) {
            ProductCatalogType.BRANDS -> {
                val e = brandDao.brandById(itemId) ?: error("Brand not found")
                brandDao.insert(e.copy(image_id = apiImage.id, synced = 0))
            }
            ProductCatalogType.CATEGORIES -> {
                val e = categoryDao.categoryById(itemId) ?: error("Category not found")
                categoryDao.insert(e.copy(image_id = apiImage.id, synced = 0))
            }
            ProductCatalogType.SUB_CATEGORIES -> {
                val e = subCategoryDao.subCategoryById(itemId) ?: error("Sub-category not found")
                subCategoryDao.insert(e.copy(image_id = apiImage.id, synced = 0))
            }
            ProductCatalogType.GROUPS -> {
                val e = groupDao.groupById(itemId) ?: error("Group not found")
                groupDao.insert(e.copy(image_id = apiImage.id, synced = 0))
            }
        }

        imageEntity.imageUrl()
    }

    suspend fun removeItemImage(type: ProductCatalogType, itemId: String): Result<Unit> = runCatching {
        when (type) {
            ProductCatalogType.BRANDS -> {
                val e = brandDao.brandById(itemId) ?: error("Brand not found")
                brandDao.insert(e.copy(image_id = null, synced = 0))
            }
            ProductCatalogType.CATEGORIES -> {
                val e = categoryDao.categoryById(itemId) ?: error("Category not found")
                categoryDao.insert(e.copy(image_id = null, synced = 0))
            }
            ProductCatalogType.SUB_CATEGORIES -> {
                val e = subCategoryDao.subCategoryById(itemId) ?: error("Sub-category not found")
                subCategoryDao.insert(e.copy(image_id = null, synced = 0))
            }
            ProductCatalogType.GROUPS -> {
                val e = groupDao.groupById(itemId) ?: error("Group not found")
                groupDao.insert(e.copy(image_id = null, synced = 0))
            }
        }
    }
}

private fun ImageEntity.imageUrl(): String =
    ApiUrlBuilder.buildCompleteUrl(object_key)

data class CatalogItem(
    val id: String,
    val name: String,
    val active: Boolean,
    val imageUrl: String? = null,
)
