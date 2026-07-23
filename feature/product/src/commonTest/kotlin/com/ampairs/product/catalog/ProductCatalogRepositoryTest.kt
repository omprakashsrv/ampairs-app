package com.ampairs.product.catalog

import app.cash.turbine.test
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FilePickerResult
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
import com.ampairs.sync.db.SyncPersistStatus
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Local-only catalog access ([ProductCatalogRepository]). The catalog API is owned by the sync
 * layer; this repo only writes Room (`synced = 0`) and flags PRODUCT_CATALOG pending-push.
 * The reuse of the `FakeBrandDao` etc. here is intentionally minimal — they are defined privately
 * in this file (test sources don't share private fakes across files).
 */
class ProductCatalogRepositoryTest {

    private val brandDao = FakeBrandDao()
    private val categoryDao = FakeCategoryDao()
    private val subCategoryDao = FakeSubCategoryDao()
    private val groupDao = FakeGroupDao()
    private val fileRepository = FakeFileRepository()
    private val syncStateDao = RecordingSyncStateDao()

    private val repository = ProductCatalogRepository(
        brandDao = brandDao,
        categoryDao = categoryDao,
        subCategoryDao = subCategoryDao,
        groupDao = groupDao,
        fileRepository = fileRepository,
        syncStateDao = syncStateDao,
    )

    @Test
    fun `createItem inserts unsynced brand and flags pending push`() = runTest {
        val result = repository.createItem(ProductCatalogType.BRANDS, uid = "B1", name = "Acme")

        assertTrue(result.isSuccess)
        val stored = brandDao.brandById("B1")
        assertEquals("Acme", stored?.name)
        assertEquals(0, stored?.synced, "catalog writes are unsynced for the push to pick up")
        assertEquals(listOf(SyncEntity.PRODUCT_CATALOG), syncStateDao.pendingPushes)
    }

    @Test
    fun `createItem routes each type to its DAO`() = runTest {
        repository.createItem(ProductCatalogType.CATEGORIES, "C1", "Tools")
        repository.createItem(ProductCatalogType.SUB_CATEGORIES, "S1", "Power Tools")
        repository.createItem(ProductCatalogType.GROUPS, "G1", "Hardware")

        assertEquals("Tools", categoryDao.categoryById("C1")?.name)
        assertEquals("Power Tools", subCategoryDao.subCategoryById("S1")?.name)
        assertEquals("Hardware", groupDao.groupById("G1")?.name)
    }

    @Test
    fun `updateItem rewrites an existing category as unsynced and re-flags push`() = runTest {
        categoryDao.insert(CategoryEntity(id = "C1", name = "Old", active = 1, synced = 1))
        syncStateDao.pendingPushes.clear()

        val result = repository.updateItem(ProductCatalogType.CATEGORIES, id = "C1", name = "New", active = false)

        assertTrue(result.isSuccess)
        val stored = categoryDao.categoryById("C1")
        assertEquals("New", stored?.name)
        assertEquals(0, stored?.active, "active = false -> 0")
        assertEquals(0, stored?.synced, "rewritten unsynced")
        assertEquals(listOf(SyncEntity.PRODUCT_CATALOG), syncStateDao.pendingPushes)
    }

    @Test
    fun `updateItem fails when the entity does not exist`() = runTest {
        val result = repository.updateItem(ProductCatalogType.BRANDS, id = "missing", name = "X", active = true)
        assertTrue(result.isFailure, "updateItem errors via runCatching when the row is absent")
        assertTrue(syncStateDao.pendingPushes.isEmpty(), "no push flagged on a failed update")
    }

    @Test
    fun `getBrands returns only active items mapped to CatalogItem`() = runTest {
        brandDao.insert(BrandEntity(id = "B1", name = "Acme", active = 1))
        brandDao.insert(BrandEntity(id = "B2", name = "Inactive", active = 0))

        val brands = repository.getBrands()
        assertEquals(listOf("Acme"), brands.map { it.name })
        assertTrue(brands.first().active)
    }

    @Test
    fun `getItemById returns null for a missing id and the item otherwise`() = runTest {
        assertNull(repository.getItemById(ProductCatalogType.GROUPS, "nope"))
        groupDao.insert(GroupEntity(id = "G1", name = "Hardware"))
        assertEquals("Hardware", repository.getItemById(ProductCatalogType.GROUPS, "G1")?.name)
    }

    @Test
    fun `observeAllItems resolves the primary file url for each brand`() = runTest {
        brandDao.insert(BrandEntity(id = "B1", name = "Acme", active = 1))
        fileRepository.setPrimary(
            FileEntityType.BRAND, "B1",
            FileItem(
                uid = "F1", entityType = FileEntityType.BRAND, entityUid = "B1",
                fileName = "logo.png", contentType = "image/png",
                imageUrl = "https://cdn/logo.png", uploadStatus = FileUploadStatus.COMPLETED,
            ),
        )

        repository.observeAllItems(ProductCatalogType.BRANDS).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("https://cdn/logo.png", items.first().imageUrl, "COMPLETED file uses imageUrl")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAllItems prefers the local path for a still-pending upload`() = runTest {
        brandDao.insert(BrandEntity(id = "B1", name = "Acme", active = 1))
        fileRepository.setPrimary(
            FileEntityType.BRAND, "B1",
            FileItem(
                uid = "F1", entityType = FileEntityType.BRAND, entityUid = "B1",
                fileName = "logo.png", contentType = "image/png",
                imageUrl = "https://cdn/logo.png",
                localPath = "/tmp/logo.png",
                uploadStatus = FileUploadStatus.PENDING,
            ),
        )

        repository.observeAllItems(ProductCatalogType.BRANDS).test {
            assertEquals("file:///tmp/logo.png", awaitItem().first().imageUrl, "pending upload shows file://localPath")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAllItems emits empty list when no items`() = runTest {
        repository.observeAllItems(ProductCatalogType.CATEGORIES).test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllItems returns active and inactive items with null url when no file`() = runTest {
        groupDao.insert(GroupEntity(id = "G1", name = "Active", active = 1))
        groupDao.insert(GroupEntity(id = "G2", name = "Inactive", active = 0))

        val items = repository.getAllItems(ProductCatalogType.GROUPS)
        assertEquals(setOf("Active", "Inactive"), items.map { it.name }.toSet())
        assertTrue(items.all { it.imageUrl == null }, "no primary file -> null url")
        assertFalse(items.first { it.name == "Inactive" }.active)
    }
}

// ---- File fake ----

private class FakeFileRepository : FileRepository {
    private val primaries = mutableMapOf<Pair<FileEntityType, String>, MutableStateFlow<FileItem?>>()

    fun setPrimary(type: FileEntityType, uid: String, item: FileItem?) {
        primaries.getOrPut(type to uid) { MutableStateFlow(null) }.value = item
    }

    override fun observeFiles(entityType: FileEntityType, entityUid: String): Flow<List<FileItem>> =
        flowOf(emptyList())

    override fun observePrimaryFile(entityType: FileEntityType, entityUid: String): Flow<FileItem?> =
        primaries.getOrPut(entityType to entityUid) { MutableStateFlow(null) }

    override suspend fun saveLocally(
        entityType: FileEntityType,
        entityUid: String,
        result: FilePickerResult,
        isPrimary: Boolean,
    ): Result<FileItem> = Result.failure(NotImplementedError("not exercised"))

    override suspend fun deleteFile(fileUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun setPrimaryFile(entityType: FileEntityType, entityUid: String, fileUid: String): Result<Unit> =
        Result.success(Unit)
    override suspend fun pullFromServer(entityType: FileEntityType, entityUid: String): Result<Int> =
        Result.success(0)
    override suspend fun readFileBytes(fileUid: String): Result<ByteArray> =
        Result.failure(NotImplementedError("not exercised"))
}

// ---- DAO fakes (private to this file) ----

private class FakeBrandDao : BrandDao {
    private val rows = MutableStateFlow<Map<String, BrandEntity>>(emptyMap())
    override suspend fun brandById(id: String): BrandEntity? = rows.value[id]
    override suspend fun getBrands(): List<BrandEntity> = rows.value.values.sortedBy { it.name }
    override fun observeBrands(): Flow<List<BrandEntity>> = rows.map { it.values.sortedBy { b -> b.name } }
    override suspend fun unSyncedBrands(): List<BrandEntity> = rows.value.values.filter { it.synced == 0 }
    override suspend fun getActiveBrands(): List<BrandEntity> = rows.value.values.filter { it.active == 1 }
    override suspend fun getAllBrands(): List<BrandEntity> = rows.value.values.toList()
    override suspend fun getBrandsByName(searchText: String): List<BrandEntity> =
        rows.value.values.filter { it.name.contains(searchText) }
    override suspend fun insert(brand: BrandEntity) { rows.value = rows.value + (brand.id to brand) }
    override suspend fun insertAll(brands: List<BrandEntity>) { rows.value = rows.value + brands.associateBy { it.id } }
    override suspend fun update(brand: BrandEntity) { rows.value = rows.value + (brand.id to brand) }
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

private class RecordingSyncStateDao : SyncStateDao {
    val pendingPushes = mutableListOf<SyncEntity>()
    override fun observeAll(): Flow<List<SyncStateEntity>> = flowOf(emptyList())
    override fun observe(entity: SyncEntity): Flow<SyncStateEntity?> = flowOf(null)
    override suspend fun getAll(): List<SyncStateEntity> = emptyList()
    override suspend fun getPending(): List<SyncStateEntity> = emptyList()
    override suspend fun upsert(state: SyncStateEntity) {}
    override suspend fun upsertStatus(
        entity: SyncEntity,
        status: SyncPersistStatus,
        lastSyncedAt: Long?,
        pendingCount: Int,
        errorMessage: String?,
        now: Long,
    ) {}
    override suspend fun getLastSyncedAtIso(entity: SyncEntity): String? = null
    override suspend fun setLastSyncedAtIso(entity: SyncEntity, iso: String) {}
    override suspend fun markPendingPush(entity: SyncEntity, now: Long) { pendingPushes += entity }
    override suspend fun deleteAll() {}
}
