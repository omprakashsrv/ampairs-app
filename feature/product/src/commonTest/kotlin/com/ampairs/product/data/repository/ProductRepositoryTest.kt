package com.ampairs.product.data.repository

import androidx.paging.PagingSource
import app.cash.turbine.test
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductVariantDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.db.dao.VariantAttributeDao
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.entity.GroupEntity
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.entity.ProductVariantEntity
import com.ampairs.product.db.entity.SubCategoryEntity
import com.ampairs.product.db.entity.VariantAttributeEntity
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductType
import com.ampairs.product.domain.ProductVariant
import com.ampairs.sync.SyncEntity
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
 * Offline-first invariants for [ProductRepository] (the API is NOT injected; the repo only writes
 * to Room + flags PENDING_PUSH). Hand-written DAO fakes (MockK is JVM-only and breaks the native
 * test targets). Asserts the exact source behaviour:
 *   - create/update persist `synced = 0` and flag SyncEntity.PRODUCT pending-push.
 *   - delete on a synced row soft-deletes (active=0, soft_deleted=1, synced=0) + flags push.
 *   - delete on an absent row hard-deletes locally and does NOT flag a push.
 *   - reactive list join resolves category/brand names; observeProduct maps entity -> domain.
 */
class ProductRepositoryTest {

    private val productDao = FakeProductDao()
    private val variantDao = FakeProductVariantDao()
    private val attributeDao = FakeVariantAttributeDao()
    private val categoryDao = FakeCategoryDao()
    private val brandDao = FakeBrandDao()
    private val subCategoryDao = FakeSubCategoryDao()
    private val groupDao = FakeGroupDao()
    private val syncStateDao = RecordingSyncStateDao()

    private val repository = ProductRepository(
        productDao = productDao,
        variantDao = variantDao,
        attributeDao = attributeDao,
        categoryDao = categoryDao,
        brandDao = brandDao,
        subCategoryDao = subCategoryDao,
        groupDao = groupDao,
        syncStateDao = syncStateDao,
    )

    private fun product(id: String = "P1", name: String = "Widget") =
        Product(id = id, name = name, code = "C1", taxCode = "HSN", sellingPrice = 90.0, mrp = 100.0)

    @Test
    fun `createProduct persists unsynced and flags pending push`() = runTest {
        val result = repository.createProduct(product())

        assertTrue(result.isSuccess)
        val stored = productDao.productById("P1")
        assertEquals("Widget", stored?.name)
        assertEquals(0, stored?.synced, "freshly written rows must be unsynced for the push to pick them up")
        assertEquals(listOf(SyncEntity.PRODUCT), syncStateDao.pendingPushes)
    }

    @Test
    fun `createProduct generates a local id when blank`() = runTest {
        val result = repository.createProduct(product(id = ""))

        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertTrue(created.id.startsWith("local_"), "blank id is replaced with a generated local id")
        assertEquals(1, productDao.count(), "exactly one row inserted under the generated id")
    }

    @Test
    fun `updateProduct rewrites row unsynced, re-flags push, preserves seq_id`() = runTest {
        // Seed an existing row with a non-zero seq_id (simulating a previously persisted product).
        productDao.insert(product().asEntityWith(seqId = 42L))
        syncStateDao.pendingPushes.clear()

        val result = repository.updateProduct(product(name = "Widget v2"))

        assertTrue(result.isSuccess)
        val stored = productDao.productById("P1")
        assertEquals("Widget v2", stored?.name)
        assertEquals(42L, stored?.seq_id, "existing seq_id is preserved on update")
        assertEquals(0, stored?.synced)
        assertEquals(listOf(SyncEntity.PRODUCT), syncStateDao.pendingPushes)
    }

    @Test
    fun `deleteProduct soft-deletes a synced row and flags pending push`() = runTest {
        productDao.insert(product().asEntityWith(active = 1, softDeleted = 0, synced = 1))
        syncStateDao.pendingPushes.clear()

        val result = repository.deleteProduct("P1")

        assertTrue(result.isSuccess)
        val row = productDao.productById("P1")
        assertEquals(0, row?.active, "soft-delete sets active = 0")
        assertEquals(1, row?.soft_deleted, "soft-delete sets soft_deleted = 1")
        assertEquals(0, row?.synced, "soft-delete sets synced = 0 so the push sends the deletion")
        assertEquals(listOf(SyncEntity.PRODUCT), syncStateDao.pendingPushes)
    }

    @Test
    fun `deleteProduct on absent row hard-deletes locally without flagging a push`() = runTest {
        val result = repository.deleteProduct("never-existed")

        assertTrue(result.isSuccess)
        assertNull(productDao.productById("never-existed"))
        assertTrue(syncStateDao.pendingPushes.isEmpty(), "local-only rows aren't pushed")
    }

    @Test
    fun `getProduct maps entity to domain and returns null when absent`() = runTest {
        assertNull(repository.getProduct("P1"))
        repository.createProduct(product(name = "Gizmo"))

        val found = repository.getProduct("P1")
        assertEquals("Gizmo", found?.name)
        assertEquals(90.0, found?.sellingPrice)
    }

    @Test
    fun `quickCreate writes a product and returns its summary`() = runTest {
        val summary = repository.quickCreate(
            id = "P2", name = "Quick", code = "QC", sellingPrice = 50.0, mrp = 0.0,
            taxCode = "HSN", baseUnitId = null,
        )
        assertEquals("Quick", summary?.name)
        // mrp <= 0 falls back to sellingPrice per quickCreate.
        assertEquals(50.0, summary?.mrp)
        assertEquals(0, productDao.productById("P2")?.synced)
        assertEquals(listOf(SyncEntity.PRODUCT), syncStateDao.pendingPushes)
    }

    @Test
    fun `observeProducts emits active products joined with category and brand names`() = runTest {
        categoryDao.seed(CategoryEntity(id = "cat1", name = "Tools"))
        brandDao.seed(BrandEntity(id = "br1", name = "Acme"))
        productDao.insert(
            product().asEntityWith().copy(category_id = "cat1", brand_id = "br1"),
        )

        repository.observeProducts().test {
            val emitted = awaitItem()
            assertEquals(1, emitted.size)
            assertEquals("Tools", emitted.first().categoryName)
            assertEquals("Acme", emitted.first().brandName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeProduct maps the entity stream to a domain product`() = runTest {
        repository.createProduct(product(name = "Streamed"))

        repository.observeProduct("P1").test {
            assertEquals("Streamed", awaitItem()?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchProducts with blank query returns all active products`() = runTest {
        productDao.insert(product(id = "A", name = "Apple").asEntityWith())
        productDao.insert(product(id = "B", name = "Banana").asEntityWith())

        repository.searchProducts("  ").test {
            assertEquals(setOf("Apple", "Banana"), awaitItem().map { it.name }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchProducts filters by name substring`() = runTest {
        productDao.insert(product(id = "A", name = "Apple").asEntityWith())
        productDao.insert(product(id = "B", name = "Banana").asEntityWith())

        repository.searchProducts("ppl").test {
            assertEquals(listOf("Apple"), awaitItem().map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProductCount reflects inserted rows`() = runTest {
        assertEquals(0, repository.getProductCount())
        repository.createProduct(product(id = "A"))
        repository.createProduct(product(id = "B"))
        assertEquals(2, repository.getProductCount())
    }

    @Test
    fun `getProductsByCategory resolves names from the category and brand DAOs`() = runTest {
        categoryDao.seed(CategoryEntity(id = "cat1", name = "Tools"))
        brandDao.seed(BrandEntity(id = "br1", name = "Acme"))
        productDao.insert(product(id = "A").asEntityWith().copy(category_id = "cat1", brand_id = "br1"))

        val items = repository.getProductsByCategory(listOf("cat1"))
        assertEquals(1, items.size)
        assertEquals("Tools", items.first().categoryName)
        assertEquals("Acme", items.first().brandName)
    }

    @Test
    fun `clearCache deletes all products`() = runTest {
        repository.createProduct(product(id = "A"))
        repository.clearCache()
        assertEquals(0, productDao.count())
    }

    // ---- variants ----

    @Test
    fun `createVariant requires id and sku, stamps timestamps, marks unsynced`() = runTest {
        val variant = ProductVariant(id = "V1", productId = "P1", sku = "SKU1", variantName = "L")

        val result = repository.createVariant(variant)

        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertFalse(saved.synced)
        assertTrue(saved.createdAt != null && saved.updatedAt != null, "timestamps are stamped")
        assertEquals(0, variantDao.getVariantById("V1")?.synced)
    }

    @Test
    fun `createVariant fails when id is blank`() = runTest {
        val result = repository.createVariant(
            ProductVariant(id = "", productId = "P1", sku = "SKU1", variantName = "L"),
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `createVariant fails when sku is blank`() = runTest {
        val result = repository.createVariant(
            ProductVariant(id = "V1", productId = "P1", sku = "", variantName = "L"),
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `createVariant records searchable attributes`() = runTest {
        val variant = ProductVariant(
            id = "V1", productId = "P1", sku = "SKU1", variantName = "L",
            attribute1Name = "Size", attribute1Value = "L",
            attribute2Name = "Color", attribute2Value = "Blue",
        )
        repository.createVariant(variant)
        assertEquals(setOf("Size", "Color"), repository.getAttributeNames("P1").toSet())
        assertEquals(listOf("L"), repository.getAttributeValues("P1", "Size"))
    }

    @Test
    fun `updateVariant rewrites unsynced`() = runTest {
        repository.createVariant(ProductVariant(id = "V1", productId = "P1", sku = "SKU1", variantName = "L"))
        val result = repository.updateVariant(
            ProductVariant(id = "V1", productId = "P1", sku = "SKU1", variantName = "XL", synced = true),
        )
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().synced, "update always re-marks unsynced regardless of input flag")
        assertEquals("XL", variantDao.getVariantById("V1")?.variantName)
    }

    @Test
    fun `deleteVariant soft-deletes the variant`() = runTest {
        repository.createVariant(ProductVariant(id = "V1", productId = "P1", sku = "SKU1", variantName = "L"))
        val result = repository.deleteVariant("V1")
        assertTrue(result.isSuccess)
        assertEquals(0, variantDao.getVariantById("V1")?.active, "soft delete sets active = 0")
    }

    @Test
    fun `observeProductVariants emits active variants for the product`() = runTest {
        repository.createVariant(ProductVariant(id = "V1", productId = "P1", sku = "S1", variantName = "L"))

        repository.observeProductVariants("P1").test {
            assertEquals(listOf("V1"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getVariantBySku returns the matching variant`() = runTest {
        repository.createVariant(ProductVariant(id = "V1", productId = "P1", sku = "SKU-X", variantName = "L"))
        assertEquals("V1", repository.getVariantBySku("SKU-X")?.id)
        assertNull(repository.getVariantBySku("missing"))
    }

    @Test
    fun `getTotalVariantStock sums active variants`() = runTest {
        repository.createVariant(ProductVariant(id = "V1", productId = "P1", sku = "S1", variantName = "L", stockQuantity = 3.0))
        repository.createVariant(ProductVariant(id = "V2", productId = "P1", sku = "S2", variantName = "M", stockQuantity = 4.0))
        assertEquals(7.0, repository.getTotalVariantStock("P1"))
    }
}

// ---- helpers ----

private fun Product.asEntityWith(
    seqId: Long = 0,
    active: Int = 1,
    softDeleted: Int = 0,
    synced: Int = 0,
): ProductEntity = ProductEntity(
    seq_id = seqId,
    id = id,
    name = name,
    code = code,
    tax_code = taxCode,
    mrp = mrp,
    dp = dp,
    selling_price = sellingPrice,
    active = active,
    soft_deleted = softDeleted,
    synced = synced,
    product_type = (productType ?: ProductType.RETAIL).name,
)

// ---- DAO fakes ----

/** In-memory [ProductDao]; reactive queries derive from one state flow. Paging methods are unused. */
private class FakeProductDao : ProductDao {
    private val rows = MutableStateFlow<Map<String, ProductEntity>>(emptyMap())

    fun count(): Int = rows.value.size

    override fun observeAllProducts(): Flow<List<ProductEntity>> =
        rows.map { m -> m.values.filter { it.active == 1 }.sortedBy { it.name } }

    override fun observeProductsByName(query: String): Flow<List<ProductEntity>> =
        rows.map { m -> m.values.filter { it.active == 1 && it.name.contains(query) }.sortedBy { it.name } }

    override fun filterProducts(
        query: String,
        brands: List<String>,
        hasBrands: Int,
        categories: List<String>,
        hasCategories: Int,
        subCategories: List<String>,
        hasSubCategories: Int,
        groups: List<String>,
        hasGroups: Int,
    ): Flow<List<ProductEntity>> = rows.map { m ->
        m.values.filter {
            it.active == 1 &&
                (hasBrands == 0 || it.brand_id in brands) &&
                (hasCategories == 0 || it.category_id in categories) &&
                (hasSubCategories == 0 || it.sub_category_id in subCategories) &&
                (hasGroups == 0 || it.group_id in groups) &&
                (query == "" || it.name.contains(query))
        }.sortedBy { it.name }
    }

    override suspend fun getDistinctBrandIds(): List<String> =
        rows.value.values.filter { it.active == 1 }.mapNotNull { it.brand_id }.filter { it.isNotEmpty() }.distinct()

    override suspend fun getDistinctCategoryIds(): List<String> =
        rows.value.values.filter { it.active == 1 }.mapNotNull { it.category_id }.filter { it.isNotEmpty() }.distinct()

    override suspend fun getDistinctSubCategoryIds(): List<String> =
        rows.value.values.filter { it.active == 1 }.mapNotNull { it.sub_category_id }.filter { it.isNotEmpty() }.distinct()

    override suspend fun getDistinctGroupIds(): List<String> =
        rows.value.values.filter { it.active == 1 }.mapNotNull { it.group_id }.filter { it.isNotEmpty() }.distinct()

    override suspend fun searchForEntry(term: String, limit: Long): List<ProductEntity> =
        rows.value.values.filter {
            it.active == 1 && (it.name.contains(term) || it.code.contains(term) || it.tax_code.startsWith(term))
        }.sortedBy { it.name }.take(limit.toInt())

    override suspend fun headProducts(limit: Long): List<ProductEntity> =
        rows.value.values.filter { it.active == 1 }.sortedBy { it.name }.take(limit.toInt())

    override fun observeProductById(id: String): Flow<ProductEntity?> = rows.map { it[id] }

    override suspend fun productById(id: String): ProductEntity? = rows.value[id]

    override suspend fun getProductsByName(searchText: String): List<ProductEntity> =
        rows.value.values.filter { it.active == 1 && it.name.contains(searchText) }.sortedBy { it.name }

    override suspend fun getProductsByNameAndGroup(searchText: String, groups: List<String>): List<ProductEntity> =
        rows.value.values.filter { it.active == 1 && it.name.contains(searchText) && it.group_id in groups }

    override suspend fun getMaxLastUpdated(): Long? = rows.value.values.mapNotNull { it.last_updated }.maxOrNull()

    override suspend fun getProducts(): List<ProductEntity> = rows.value.values.toList()

    override suspend fun productsByCategoryIds(categoryIds: List<String>): List<ProductEntity> =
        rows.value.values.filter { it.active == 1 && it.category_id in categoryIds }

    override suspend fun productCategories(groupId: String): List<String> =
        rows.value.values.filter { it.group_id == groupId && it.active == 1 }.mapNotNull { it.category_id }.distinct()

    override suspend fun countProductsByGroupAndCategory(groupId: String, categoryId: String): Int =
        rows.value.values.count { it.group_id == groupId && it.category_id == categoryId }

    override suspend fun productsByGroupAndCategory(groupId: String, categoryId: String, limit: Long, offset: Long): List<ProductEntity> =
        rows.value.values.filter { it.group_id == groupId && it.category_id == categoryId }

    override suspend fun productsByIds(ids: List<String>): List<ProductEntity> =
        rows.value.values.filter { it.id in ids }

    override suspend fun countProductsByName(name: String): Int =
        rows.value.values.count { it.name.contains(name) }

    override suspend fun productsByName(name: String, limit: Long, offset: Long): List<ProductEntity> =
        rows.value.values.filter { it.name.contains(name) }

    override suspend fun countProducts(): Int = rows.value.size

    override suspend fun products(limit: Long, offset: Long): List<ProductEntity> = rows.value.values.toList()

    override suspend fun unSyncedProducts(): List<ProductEntity> = rows.value.values.filter { it.synced == 0 }

    override fun observeUnsyncedCount(): Flow<Int> = rows.map { m -> m.values.count { it.synced == 0 } }

    override suspend fun insert(product: ProductEntity) {
        rows.value = rows.value + (product.id to product)
    }

    override suspend fun insertAll(products: List<ProductEntity>) {
        rows.value = rows.value + products.associateBy { it.id }
    }

    override suspend fun update(product: ProductEntity) {
        rows.value = rows.value + (product.id to product)
    }

    override suspend fun getProductsByTallyRefIds(refs: List<String>): List<ProductEntity> =
        rows.value.values.filter { it.ref_id in refs }

    override suspend fun updateStockQuantity(id: String, quantity: Double) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(stock_quantity = quantity)) }
    }

    override suspend fun updateStockQuantityByTallyRef(tallyRefId: String, quantity: Double) {
        rows.value = rows.value.mapValues { (_, v) ->
            if (v.ref_id == tallyRefId) v.copy(stock_quantity = quantity) else v
        }
    }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun deleteAll() {
        rows.value = emptyMap()
    }

    override fun getProductPagingSource(searchText: String): PagingSource<Int, ProductEntity> =
        throw NotImplementedError("paging not exercised in unit tests")

    override fun getAllProductsPagingSource(): PagingSource<Int, ProductEntity> =
        throw NotImplementedError("paging not exercised in unit tests")

    override fun getProductPagingSourceByGroupAndCategory(groupId: String, categoryId: String): PagingSource<Int, ProductEntity> =
        throw NotImplementedError("paging not exercised in unit tests")
}

private class FakeProductVariantDao : ProductVariantDao {
    private val rows = MutableStateFlow<Map<String, ProductVariantEntity>>(emptyMap())

    override fun observeProductVariants(productId: String): Flow<List<ProductVariantEntity>> =
        rows.map { m -> m.values.filter { it.productId == productId && it.active == 1 }.sortedBy { it.variantName } }

    override suspend fun getVariantById(variantId: String): ProductVariantEntity? = rows.value[variantId]

    override suspend fun getVariantBySku(sku: String): ProductVariantEntity? =
        rows.value.values.firstOrNull { it.sku == sku }

    override suspend fun getProductVariants(productId: String): List<ProductVariantEntity> =
        rows.value.values.filter { it.productId == productId && it.active == 1 }.sortedBy { it.variantName }

    override suspend fun getTotalProductStock(productId: String): Double =
        rows.value.values.filter { it.productId == productId && it.active == 1 }.sumOf { it.stockQuantity }

    override suspend fun insertVariant(variant: ProductVariantEntity) {
        rows.value = rows.value + (variant.id to variant)
    }

    override suspend fun insertVariants(variants: List<ProductVariantEntity>) {
        rows.value = rows.value + variants.associateBy { it.id }
    }

    override suspend fun updateVariant(variant: ProductVariantEntity) {
        rows.value = rows.value + (variant.id to variant)
    }

    override suspend fun deleteVariant(variantId: String) {
        rows.value[variantId]?.let { rows.value = rows.value + (variantId to it.copy(active = 0)) }
    }

    override suspend fun permanentlyDeleteVariant(variantId: String) {
        rows.value = rows.value - variantId
    }

    override suspend fun getUnsyncedVariants(): List<ProductVariantEntity> =
        rows.value.values.filter { it.synced == 0 }

    override suspend fun updateSyncStatus(variantId: String, synced: Boolean) {
        rows.value[variantId]?.let {
            rows.value = rows.value + (variantId to it.copy(synced = if (synced) 1 else 0))
        }
    }
}

private class FakeVariantAttributeDao : VariantAttributeDao {
    private val rows = mutableListOf<VariantAttributeEntity>()

    override suspend fun getAttributeValues(productId: String, attributeName: String): List<String> =
        rows.filter { it.productId == productId && it.attributeName == attributeName }
            .map { it.attributeValue }.distinct().sorted()

    override suspend fun getAttributeNames(productId: String): List<String> =
        rows.filter { it.productId == productId }.map { it.attributeName }.distinct().sorted()

    override suspend fun insertAttribute(attribute: VariantAttributeEntity) {
        rows.add(attribute)
    }

    override suspend fun insertAttributes(attributes: List<VariantAttributeEntity>) {
        rows.addAll(attributes)
    }

    override suspend fun deleteProductAttributes(productId: String) {
        rows.removeAll { it.productId == productId }
    }

    override suspend fun deleteAttributeValue(productId: String, attributeName: String, attributeValue: String) {
        rows.removeAll {
            it.productId == productId && it.attributeName == attributeName && it.attributeValue == attributeValue
        }
    }
}

private class FakeCategoryDao : CategoryDao {
    private val rows = MutableStateFlow<Map<String, CategoryEntity>>(emptyMap())
    fun seed(e: CategoryEntity) { rows.value = rows.value + (e.id to e) }

    override suspend fun categoryById(id: String): CategoryEntity? = rows.value[id]
    override suspend fun getCategories(): List<CategoryEntity> = rows.value.values.sortedBy { it.name }
    override fun observeCategories(): Flow<List<CategoryEntity>> = rows.map { it.values.sortedBy { c -> c.name } }
    override suspend fun getCategoriesByIds(ids: List<String>): List<CategoryEntity> =
        rows.value.values.filter { it.id in ids }
    override suspend fun unSyncedCategories(): List<CategoryEntity> = rows.value.values.filter { it.synced == 0 }
    override suspend fun getActiveCategories(): List<CategoryEntity> = rows.value.values.filter { it.active == 1 }
    override suspend fun getAllCategoryEntities(): List<CategoryEntity> = rows.value.values.toList()
    override suspend fun getCategoriesByName(searchText: String): List<CategoryEntity> =
        rows.value.values.filter { it.name.contains(searchText) }
    override suspend fun getCategoriesByTallyRefIds(refs: List<String>): List<CategoryEntity> =
        rows.value.values.filter { it.ref_id in refs }
    override suspend fun insert(category: CategoryEntity) { rows.value = rows.value + (category.id to category) }
    override suspend fun insertAll(categories: List<CategoryEntity>) { rows.value = rows.value + categories.associateBy { it.id } }
    override suspend fun update(category: CategoryEntity) { rows.value = rows.value + (category.id to category) }
    override suspend fun deleteById(id: String) { rows.value = rows.value - id }
    override suspend fun deleteAll() { rows.value = emptyMap() }
    override suspend fun markAsSynced(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(synced = 1)) }
    }
    override suspend fun softDelete(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(soft_deleted = 1)) }
    }
    override suspend fun updateActiveStatus(id: String, active: Int) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = active)) }
    }
}

private class FakeBrandDao : BrandDao {
    private val rows = MutableStateFlow<Map<String, BrandEntity>>(emptyMap())
    fun seed(e: BrandEntity) { rows.value = rows.value + (e.id to e) }

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
    override suspend fun markAsSynced(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(synced = 1)) }
    }
    override suspend fun softDelete(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(soft_deleted = 1)) }
    }
    override suspend fun updateActiveStatus(id: String, active: Int) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = active)) }
    }
}

private class FakeSubCategoryDao : SubCategoryDao {
    private val rows = MutableStateFlow<Map<String, SubCategoryEntity>>(emptyMap())

    override suspend fun subCategoryById(id: String): SubCategoryEntity? = rows.value[id]
    override suspend fun getSubCategories(): List<SubCategoryEntity> = rows.value.values.sortedBy { it.name }
    override fun observeSubCategories(): Flow<List<SubCategoryEntity>> = rows.map { it.values.sortedBy { s -> s.name } }
    override suspend fun unSyncedSubCategories(): List<SubCategoryEntity> = rows.value.values.filter { it.synced == 0 }
    override suspend fun getActiveSubCategories(): List<SubCategoryEntity> = rows.value.values.filter { it.active == 1 }
    override suspend fun getAllSubCategoryEntities(): List<SubCategoryEntity> = rows.value.values.toList()
    override suspend fun getSubCategoriesByName(searchText: String): List<SubCategoryEntity> =
        rows.value.values.filter { it.name.contains(searchText) }
    override suspend fun insert(subCategory: SubCategoryEntity) { rows.value = rows.value + (subCategory.id to subCategory) }
    override suspend fun insertAll(subCategories: List<SubCategoryEntity>) { rows.value = rows.value + subCategories.associateBy { it.id } }
    override suspend fun update(subCategory: SubCategoryEntity) { rows.value = rows.value + (subCategory.id to subCategory) }
    override suspend fun deleteById(id: String) { rows.value = rows.value - id }
    override suspend fun deleteAll() { rows.value = emptyMap() }
    override suspend fun markAsSynced(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(synced = 1)) }
    }
    override suspend fun softDelete(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(soft_deleted = 1)) }
    }
    override suspend fun updateActiveStatus(id: String, active: Int) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = active)) }
    }
}

private class FakeGroupDao : GroupDao {
    private val rows = MutableStateFlow<Map<String, GroupEntity>>(emptyMap())

    override suspend fun groupById(id: String): GroupEntity? = rows.value[id]
    override suspend fun getGroups(): List<GroupEntity> = rows.value.values.sortedBy { it.name }
    override fun observeGroups(): Flow<List<GroupEntity>> = rows.map { it.values.sortedBy { g -> g.name } }
    override suspend fun unSyncedGroups(): List<GroupEntity> = rows.value.values.filter { it.synced == 0 }
    override suspend fun getActiveGroups(): List<GroupEntity> = rows.value.values.filter { it.active == 1 }
    override suspend fun getGroupsByName(searchText: String): List<GroupEntity> =
        rows.value.values.filter { it.name.contains(searchText) }
    override suspend fun getGroupsByTallyRefIds(refs: List<String>): List<GroupEntity> =
        rows.value.values.filter { it.ref_id in refs }
    override suspend fun insert(group: GroupEntity) { rows.value = rows.value + (group.id to group) }
    override suspend fun insertAll(groups: List<GroupEntity>) { rows.value = rows.value + groups.associateBy { it.id } }
    override suspend fun update(group: GroupEntity) { rows.value = rows.value + (group.id to group) }
    override suspend fun deleteById(id: String) { rows.value = rows.value - id }
    override suspend fun deleteAll() { rows.value = emptyMap() }
    override suspend fun markAsSynced(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(synced = 1)) }
    }
    override suspend fun softDelete(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(soft_deleted = 1)) }
    }
    override suspend fun updateActiveStatus(id: String, active: Int) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(active = active)) }
    }
}

/** Records markPendingPush calls so tests can assert the offline-sync flag. */
private class RecordingSyncStateDao : SyncStateDao {
    val pendingPushes = mutableListOf<SyncEntity>()

    override fun observeAll(): Flow<List<SyncStateEntity>> = flowOf(emptyList())
    override fun observe(entity: SyncEntity): Flow<SyncStateEntity?> = flowOf(null)
    override suspend fun getAll(): List<SyncStateEntity> = emptyList()
    override suspend fun getPending(): List<SyncStateEntity> = emptyList()
    override suspend fun upsert(state: SyncStateEntity) {}
    override suspend fun getLastSyncedAtIso(entity: SyncEntity): String? = null
    override suspend fun setLastSyncedAtIso(entity: SyncEntity, iso: String) {}
    override suspend fun markPendingPush(entity: SyncEntity, now: Long) { pendingPushes += entity }
    override suspend fun deleteAll() {}
}
