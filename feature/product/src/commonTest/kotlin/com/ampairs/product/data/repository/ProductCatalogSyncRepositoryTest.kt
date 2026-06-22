package com.ampairs.product.data.repository

import com.ampairs.common.model.PageResponse
import com.ampairs.product.api.model.ProductGroupApiModel
import com.ampairs.product.data.api.ProductCatalogApi
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.SubCategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [ProductCatalogSyncRepository] is the catalog sync owner that DOES hold the API. These tests pin
 * its push/pull bookkeeping with a hand-written fake [ProductCatalogApi]:
 *   - pull pages through `hasNext` and upserts each batch (synced via mappers).
 *   - push sends only unsynced rows and marks them synced on success.
 *   - push/pull aggregate the per-type counts; a failing API call surfaces as Result.failure.
 */
class ProductCatalogSyncRepositoryTest {

    private val api = FakeProductCatalogApi()
    private val groupDao = FakeGroupDao()
    private val categoryDao = FakeCategoryDao()
    private val brandDao = FakeBrandDao()
    private val subCategoryDao = FakeSubCategoryDao()

    private val repository = ProductCatalogSyncRepository(
        api = api,
        groupDao = groupDao,
        categoryDao = categoryDao,
        brandDao = brandDao,
        subCategoryDao = subCategoryDao,
    )

    private fun model(id: String, name: String) = ProductGroupApiModel(id = id, name = name)

    private fun page(items: List<ProductGroupApiModel>, hasNext: Boolean) = PageResponse(
        content = items,
        pageNumber = 0,
        pageSize = items.size,
        totalPages = 1,
        totalElements = items.size.toLong(),
        hasNext = hasNext,
        hasPrevious = false,
        first = true,
        last = !hasNext,
    )

    @Test
    fun `pull upserts one page from each catalog type and totals the counts`() = runTest {
        api.groupPages = listOf(page(listOf(model("g1", "Hardware")), hasNext = false))
        api.categoryPages = listOf(page(listOf(model("c1", "Tools")), hasNext = false))
        api.brandPages = listOf(page(listOf(model("b1", "Acme"), model("b2", "Globex")), hasNext = false))
        api.subCategoryPages = listOf(page(emptyList(), hasNext = false))

        val result = repository.pullFromServer()

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrThrow(), "1 group + 1 category + 2 brands + 0 sub = 4")
        assertEquals("Hardware", groupDao.groupById("g1")?.name)
        assertEquals("Acme", brandDao.brandById("b1")?.name)
        assertEquals(1, brandDao.brandById("b1")?.synced, "pulled rows are stored synced = 1")
    }

    @Test
    fun `pull follows hasNext across multiple pages`() = runTest {
        api.groupPages = listOf(
            page(listOf(model("g1", "A")), hasNext = true),
            page(listOf(model("g2", "B")), hasNext = false),
        )
        api.categoryPages = listOf(page(emptyList(), hasNext = false))
        api.brandPages = listOf(page(emptyList(), hasNext = false))
        api.subCategoryPages = listOf(page(emptyList(), hasNext = false))

        val result = repository.pullFromServer()

        assertEquals(2, result.getOrThrow(), "both group pages consumed")
        assertEquals(setOf("g1", "g2"), setOf(groupDao.groupById("g1")?.id, groupDao.groupById("g2")?.id))
    }

    @Test
    fun `pull surfaces an API failure as Result_failure`() = runTest {
        api.failGroups = true
        val result = repository.pullFromServer()
        assertTrue(result.isFailure)
    }

    @Test
    fun `push sends only unsynced rows and marks them synced`() = runTest {
        groupDao.insert(GroupEntity(id = "g1", name = "Hardware", synced = 0))
        groupDao.insert(GroupEntity(id = "g2", name = "AlreadySynced", synced = 1))
        categoryDao.insert(CategoryEntity(id = "c1", name = "Tools", synced = 0))

        val result = repository.pushPendingToServer()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow(), "only the 2 unsynced rows (g1, c1) are pushed")
        assertEquals(listOf("g1"), api.pushedGroupIds, "only the unsynced group is sent")
        assertEquals(1, groupDao.groupById("g1")?.synced, "pushed row marked synced")
        assertEquals(1, groupDao.groupById("g2")?.synced, "already-synced row untouched")
        assertEquals(1, categoryDao.categoryById("c1")?.synced)
    }

    @Test
    fun `push with nothing pending returns zero`() = runTest {
        val result = repository.pushPendingToServer()
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun `push surfaces an API failure as Result_failure and does not mark synced`() = runTest {
        groupDao.insert(GroupEntity(id = "g1", name = "Hardware", synced = 0))
        api.failPushGroups = true

        val result = repository.pushPendingToServer()

        assertTrue(result.isFailure)
        assertEquals(0, groupDao.groupById("g1")?.synced, "row stays unsynced when the push fails")
    }
}

// ---- API fake ----

private class FakeProductCatalogApi : ProductCatalogApi {
    var groupPages: List<PageResponse<ProductGroupApiModel>> = listOf(emptyPage())
    var categoryPages: List<PageResponse<ProductGroupApiModel>> = listOf(emptyPage())
    var brandPages: List<PageResponse<ProductGroupApiModel>> = listOf(emptyPage())
    var subCategoryPages: List<PageResponse<ProductGroupApiModel>> = listOf(emptyPage())

    var failGroups = false
    var failPushGroups = false

    val pushedGroupIds = mutableListOf<String>()

    override suspend fun getGroupsSync(lastSync: String?, page: Int, size: Int): Result<PageResponse<ProductGroupApiModel>> =
        if (failGroups) Result.failure(RuntimeException("boom")) else Result.success(groupPages[page])

    override suspend fun getCategoriesSync(lastSync: String?, page: Int, size: Int): Result<PageResponse<ProductGroupApiModel>> =
        Result.success(categoryPages[page])

    override suspend fun getBrandsSync(lastSync: String?, page: Int, size: Int): Result<PageResponse<ProductGroupApiModel>> =
        Result.success(brandPages[page])

    override suspend fun getSubCategoriesSync(lastSync: String?, page: Int, size: Int): Result<PageResponse<ProductGroupApiModel>> =
        Result.success(subCategoryPages[page])

    override suspend fun updateGroups(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>> {
        if (failPushGroups) return Result.failure(RuntimeException("boom"))
        pushedGroupIds += models.mapNotNull { it.id }
        return Result.success(models)
    }

    override suspend fun updateCategories(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>> =
        Result.success(models)

    override suspend fun updateBrands(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>> =
        Result.success(models)

    override suspend fun updateSubCategories(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>> =
        Result.success(models)

    companion object {
        fun emptyPage() = PageResponse<ProductGroupApiModel>(
            content = emptyList(), pageNumber = 0, pageSize = 0, totalPages = 1,
            totalElements = 0, hasNext = false, hasPrevious = false, first = true, last = true,
        )
    }
}

// ---- DAO fakes (private to this file) ----

private class FakeGroupDao : GroupDao {
    private val rows = MutableStateFlow<Map<String, GroupEntity>>(emptyMap())
    override suspend fun groupById(id: String): GroupEntity? = rows.value[id]
    override suspend fun getGroups(): List<GroupEntity> = rows.value.values.sortedBy { it.name }
    override fun observeGroups(): Flow<List<GroupEntity>> = rows.map { it.values.sortedBy { g -> g.name } }
    override suspend fun unSyncedGroups(): List<GroupEntity> = rows.value.values.filter { it.synced == 0 }
    override suspend fun getActiveGroups(): List<GroupEntity> = rows.value.values.filter { it.active == 1 }
    override suspend fun getGroupsByName(searchText: String): List<GroupEntity> = rows.value.values.filter { it.name.contains(searchText) }
    override suspend fun getGroupsByTallyRefIds(refs: List<String>): List<GroupEntity> = rows.value.values.filter { it.ref_id in refs }
    override suspend fun insert(group: GroupEntity) { rows.value = rows.value + (group.id to group) }
    override suspend fun insertAll(groups: List<GroupEntity>) { rows.value = rows.value + groups.associateBy { it.id } }
    override suspend fun update(group: GroupEntity) { rows.value = rows.value + (group.id to group) }
    override suspend fun deleteById(id: String) { rows.value = rows.value - id }
    override suspend fun deleteAll() { rows.value = emptyMap() }
    override suspend fun markAsSynced(id: String) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(synced = 1)) } }
    override suspend fun softDelete(id: String) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(soft_deleted = 1)) } }
    override suspend fun updateActiveStatus(id: String, active: Int) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = active)) } }
}

private class FakeCategoryDao : CategoryDao {
    private val rows = MutableStateFlow<Map<String, CategoryEntity>>(emptyMap())
    override suspend fun categoryById(id: String): CategoryEntity? = rows.value[id]
    override suspend fun getCategories(): List<CategoryEntity> = rows.value.values.sortedBy { it.name }
    override fun observeCategories(): Flow<List<CategoryEntity>> = rows.map { it.values.sortedBy { c -> c.name } }
    override suspend fun getCategoriesByIds(ids: List<String>): List<CategoryEntity> = rows.value.values.filter { it.id in ids }
    override suspend fun unSyncedCategories(): List<CategoryEntity> = rows.value.values.filter { it.synced == 0 }
    override suspend fun getActiveCategories(): List<CategoryEntity> = rows.value.values.filter { it.active == 1 }
    override suspend fun getAllCategoryEntities(): List<CategoryEntity> = rows.value.values.toList()
    override suspend fun getCategoriesByName(searchText: String): List<CategoryEntity> = rows.value.values.filter { it.name.contains(searchText) }
    override suspend fun getCategoriesByTallyRefIds(refs: List<String>): List<CategoryEntity> = rows.value.values.filter { it.ref_id in refs }
    override suspend fun insert(category: CategoryEntity) { rows.value = rows.value + (category.id to category) }
    override suspend fun insertAll(categories: List<CategoryEntity>) { rows.value = rows.value + categories.associateBy { it.id } }
    override suspend fun update(category: CategoryEntity) { rows.value = rows.value + (category.id to category) }
    override suspend fun deleteById(id: String) { rows.value = rows.value - id }
    override suspend fun deleteAll() { rows.value = emptyMap() }
    override suspend fun markAsSynced(id: String) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(synced = 1)) } }
    override suspend fun softDelete(id: String) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(soft_deleted = 1)) } }
    override suspend fun updateActiveStatus(id: String, active: Int) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = active)) } }
}

private class FakeBrandDao : BrandDao {
    private val rows = MutableStateFlow<Map<String, BrandEntity>>(emptyMap())
    override suspend fun brandById(id: String): BrandEntity? = rows.value[id]
    override suspend fun getBrands(): List<BrandEntity> = rows.value.values.sortedBy { it.name }
    override fun observeBrands(): Flow<List<BrandEntity>> = rows.map { it.values.sortedBy { b -> b.name } }
    override suspend fun unSyncedBrands(): List<BrandEntity> = rows.value.values.filter { it.synced == 0 }
    override suspend fun getActiveBrands(): List<BrandEntity> = rows.value.values.filter { it.active == 1 }
    override suspend fun getAllBrands(): List<BrandEntity> = rows.value.values.toList()
    override suspend fun getBrandsByName(searchText: String): List<BrandEntity> = rows.value.values.filter { it.name.contains(searchText) }
    override suspend fun insert(brand: BrandEntity) { rows.value = rows.value + (brand.id to brand) }
    override suspend fun insertAll(brands: List<BrandEntity>) { rows.value = rows.value + brands.associateBy { it.id } }
    override suspend fun update(brand: BrandEntity) { rows.value = rows.value + (brand.id to brand) }
    override suspend fun deleteById(id: String) { rows.value = rows.value - id }
    override suspend fun deleteAll() { rows.value = emptyMap() }
    override suspend fun markAsSynced(id: String) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(synced = 1)) } }
    override suspend fun softDelete(id: String) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(soft_deleted = 1)) } }
    override suspend fun updateActiveStatus(id: String, active: Int) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = active)) } }
}

private class FakeSubCategoryDao : SubCategoryDao {
    private val rows = MutableStateFlow<Map<String, SubCategoryEntity>>(emptyMap())
    override suspend fun subCategoryById(id: String): SubCategoryEntity? = rows.value[id]
    override suspend fun getSubCategories(): List<SubCategoryEntity> = rows.value.values.sortedBy { it.name }
    override fun observeSubCategories(): Flow<List<SubCategoryEntity>> = rows.map { it.values.sortedBy { s -> s.name } }
    override suspend fun unSyncedSubCategories(): List<SubCategoryEntity> = rows.value.values.filter { it.synced == 0 }
    override suspend fun getActiveSubCategories(): List<SubCategoryEntity> = rows.value.values.filter { it.active == 1 }
    override suspend fun getAllSubCategoryEntities(): List<SubCategoryEntity> = rows.value.values.toList()
    override suspend fun getSubCategoriesByName(searchText: String): List<SubCategoryEntity> = rows.value.values.filter { it.name.contains(searchText) }
    override suspend fun insert(subCategory: SubCategoryEntity) { rows.value = rows.value + (subCategory.id to subCategory) }
    override suspend fun insertAll(subCategories: List<SubCategoryEntity>) { rows.value = rows.value + subCategories.associateBy { it.id } }
    override suspend fun update(subCategory: SubCategoryEntity) { rows.value = rows.value + (subCategory.id to subCategory) }
    override suspend fun deleteById(id: String) { rows.value = rows.value - id }
    override suspend fun deleteAll() { rows.value = emptyMap() }
    override suspend fun markAsSynced(id: String) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(synced = 1)) } }
    override suspend fun softDelete(id: String) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(soft_deleted = 1)) } }
    override suspend fun updateActiveStatus(id: String, active: Int) { rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = active)) } }
}
