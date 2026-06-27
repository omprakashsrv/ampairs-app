package com.ampairs.product.data.repository

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.product.util.ProductLogger
import com.ampairs.common.cache.CacheCleanable
import com.ampairs.product.data.ProductDataService
import com.ampairs.common.components.FilterOption
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductVariantDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.db.dao.VariantAttributeDao
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.entity.VariantAttributeEntity
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductListItem
import com.ampairs.product.domain.ProductSummary
import com.ampairs.product.domain.ProductType
import com.ampairs.product.domain.ProductVariant
import com.ampairs.product.domain.ServiceType
import com.ampairs.product.domain.VariantOption
import com.ampairs.product.domain.asDomainModel
import com.ampairs.product.domain.toDomain
import com.ampairs.product.domain.toDomainList
import com.ampairs.product.domain.toEntity
import com.ampairs.product.domain.toSummary
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for products and variants. The [ProductApi] is NOT injected here —
 * all product ↔ server traffic (bulk push, batched pull, backend events) lives in
 * [com.ampairs.product.sync.ProductSyncDelegate].
 *
 * Writes (create/update/delete) persist to Room as unsynced and mark PRODUCT as PENDING_PUSH;
 * CentralSyncService's reactive observer then runs the automatic bulk push.
 */
@OptIn(ExperimentalTime::class)
@Inject
class ProductRepository(
    private val productDao: ProductDao,
    private val variantDao: ProductVariantDao,
    private val attributeDao: VariantAttributeDao,
    private val categoryDao: CategoryDao,
    private val brandDao: BrandDao,
    private val subCategoryDao: SubCategoryDao,
    private val groupDao: GroupDao,
    private val syncStateDao: SyncStateDao,
) : ProductDataService, CacheCleanable {

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.PRODUCT, Clock.System.now().toEpochMilliseconds())
    }

    fun observeProducts(): Flow<List<ProductListItem>> =
        observeProductsJoined(productDao.observeAllProducts())

    fun searchProducts(query: String): Flow<List<ProductListItem>> =
        if (query.isBlank()) {
            observeProductsJoined(productDao.observeAllProducts())
        } else {
            observeProductsJoined(productDao.observeProductsByName(query))
        }

    /** Combined search + multi-select filter (brand / category / sub-category / group). Empty list = no filter. */
    fun filterProducts(
        query: String,
        brands: List<String>,
        categories: List<String>,
        subCategories: List<String>,
        groups: List<String>,
    ): Flow<List<ProductListItem>> = observeProductsJoined(
        productDao.filterProducts(
            query = query,
            brands = brands,
            hasBrands = if (brands.isEmpty()) 0 else 1,
            categories = categories,
            hasCategories = if (categories.isEmpty()) 0 else 1,
            subCategories = subCategories,
            hasSubCategories = if (subCategories.isEmpty()) 0 else 1,
            groups = groups,
            hasGroups = if (groups.isEmpty()) 0 else 1,
        )
    )

    /** Filter options derived from values actually present among active products; labels resolved to names. */
    suspend fun getBrandFilterOptions(): List<FilterOption> {
        val ids = productDao.getDistinctBrandIds()
        val names = brandDao.getBrands().associate { it.id to it.name }
        return ids.map { FilterOption(it, names[it] ?: it) }
    }

    suspend fun getCategoryFilterOptions(): List<FilterOption> {
        val ids = productDao.getDistinctCategoryIds()
        val names = categoryDao.getCategories().associate { it.id to it.name }
        return ids.map { FilterOption(it, names[it] ?: it) }
    }

    suspend fun getSubCategoryFilterOptions(): List<FilterOption> {
        val ids = productDao.getDistinctSubCategoryIds()
        val names = subCategoryDao.getSubCategories().associate { it.id to it.name }
        return ids.map { FilterOption(it, names[it] ?: it) }
    }

    suspend fun getGroupFilterOptions(): List<FilterOption> {
        val ids = productDao.getDistinctGroupIds()
        val names = groupDao.getGroups().associate { it.id to it.name }
        return ids.map { FilterOption(it, names[it] ?: it) }
    }

    private fun observeProductsJoined(
        productsFlow: Flow<List<ProductEntity>>,
    ): Flow<List<ProductListItem>> = combine(
        productsFlow,
        categoryDao.observeCategories(),
        brandDao.observeBrands(),
    ) { products, categories, brands ->
        val categoryNames = categories.associate { it.id to it.name }
        val brandNames = brands.associate { it.id to it.name }
        products.map { it.toProductListItem(categoryNames, brandNames) }
    }

    fun observeProduct(productId: String): Flow<Product?> =
        productDao.observeProductById(productId).map { entity -> entity?.toDomainProduct() }

    /**
     * Get products by category
     */
    suspend fun getProductsByCategory(categoryIds: List<String>): List<ProductListItem> {
        val products = productDao.productsByCategoryIds(categoryIds)
        val categoryNames = categoryDao.getCategoriesByIds(categoryIds).associate { it.id to it.name }
        val brandIds = products.mapNotNull { it.brand_id }.distinct()
        val brandNames = if (brandIds.isEmpty()) emptyMap()
        else brandDao.getBrands().filter { it.id in brandIds }.associate { it.id to it.name }
        return products.map { it.toProductListItem(categoryNames, brandNames) }
    }

    /**
     * Get a single product (suspending)
     */
    suspend fun getProduct(productId: String): Product? {
        return productDao.productById(productId)?.let { entity ->
            entity.toDomainProduct()
        }
    }

    override suspend fun getById(uid: String): ProductSummary? = getProduct(uid)?.toSummary()

    override suspend fun getByIds(ids: List<String>): List<ProductSummary> =
        productDao.productsByIds(ids).map { it.asDomainModel().toSummary() }

    override suspend fun searchSummaries(term: String, limit: Int): List<ProductSummary> {
        val entities = if (term.isBlank()) {
            productDao.headProducts(limit.toLong())
        } else {
            val normalizedTerm = normalizeWhitespace(term)
            // Try word-based search first, fall back to substring search
            val wordResults = productDao.searchByWords(normalizedTerm, limit.toLong())
            if (wordResults.isNotEmpty()) {
                wordResults
            } else {
                productDao.searchForEntry(normalizedTerm, limit.toLong())
            }
        }
        return entities.map { it.asDomainModel().toSummary() }
    }

    private fun normalizeWhitespace(text: String): String {
        return text.trim().replace(Regex("\\s+"), " ")
    }

    override suspend fun variantsForProduct(productId: String): List<VariantOption> =
        variantDao.getProductVariants(productId)
            .filter { it.active == 1 }
            .map { v ->
                VariantOption(
                    sku = v.sku,
                    label = v.variantName,
                    sellingPrice = v.sellingPrice,
                )
            }

    override suspend fun clearCache() { productDao.deleteAll() }

    override suspend fun quickCreate(
        id: String,
        name: String,
        code: String,
        sellingPrice: Double,
        mrp: Double,
        taxCode: String,
        baseUnitId: String?,
    ): ProductSummary? {
        require(id.isNotBlank()) { "Product UID must be set by the ViewModel" }
        val product = Product(
            id = id,
            name = name,
            code = code,
            sellingPrice = sellingPrice,
            dp = sellingPrice,
            mrp = if (mrp > 0.0) mrp else sellingPrice,
            taxCode = taxCode,
            baseUnitId = baseUnitId,
            active = true,
            productType = ProductType.RETAIL,
        )
        return createProduct(product).getOrNull()?.toSummary()
    }

    suspend fun createProduct(product: Product): Result<Product> {
        return try {
            val productWithId = if (product.id.isBlank()) {
                product.copy(id = generateLocalId())
            } else product

            productDao.insert(productWithId.toEntity())
            markPending()
            Result.success(productWithId)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.createProduct")
            Result.failure(e)
        }
    }

    /**
     * Update an existing product
     */
    suspend fun updateProduct(product: Product): Result<Product> {
        return try {
            // Get existing entity to preserve seq_id
            val existing = productDao.productById(product.id)
            val entityToUpdate = if (existing != null) {
                product.toEntity().copy(seq_id = existing.seq_id)
            } else {
                product.toEntity()
            }
            productDao.update(entityToUpdate)
            markPending()
            Result.success(product)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.updateProduct")
            Result.failure(e)
        }
    }

    /**
     * Delete a product. Soft-deletes locally (active = 0, soft_deleted = 1, synced = 0) and flags
     * PRODUCT pending-push so the bulk sync sends it to the server (status = DELETED). The delegate
     * then hard-deletes the local row once the server confirms; the server-side delete also unlists
     * the product from the ecom storefront.
     */
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            val existing = productDao.productById(productId)
            if (existing == null) {
                // Never synced to the server (local-only) — just drop it locally.
                productDao.deleteById(productId)
            } else {
                productDao.insert(existing.copy(active = 0, soft_deleted = 1, synced = 0))
                markPending()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.deleteProduct")
            Result.failure(e)
        }
    }

    private fun generateLocalId(): String {
        return "local_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }

    suspend fun getProductCount(): Int {
        return productDao.countProducts()
    }

    // Extension functions for data conversion
    private fun Product.toEntity(): ProductEntity {
        val now = Clock.System.now()
        return ProductEntity(
            id = this.id,
            name = this.name,
            code = this.code,
            group_id = this.groupId.takeIf { it.isNotBlank() },
            brand_id = this.brandId.takeIf { it.isNotBlank() },
            category_id = this.categoryId.takeIf { it.isNotBlank() },
            sub_category_id = this.subCategoryId.takeIf { it.isNotBlank() },
            active = if (this.active) 1 else 0,
            tax_code = this.taxCode,
            mrp = this.mrp,
            dp = this.dp,
            selling_price = this.sellingPrice,
            description = this.description,
            stock_quantity = this.stockQuantity,
            low_stock_alert = this.lowStockAlert,
            base_unit = this.baseUnitId,
            product_type = this.productType?.name,
            service_type = this.serviceType?.name,
            has_variants = if (this.hasVariants) 1 else 0,
            last_updated = now.toEpochMilliseconds(),
            created_at = now.toString(),
            updated_at = now.toString(),
            synced = 0 // Mark as unsynced initially
        )
    }

    private suspend fun ProductEntity.toDomainProduct(): Product {
        // Load variants if product has them
        val variants = if (this.has_variants == 1) {
            variantDao.getProductVariants(this.id).map { it.toDomain() }
        } else {
            null
        }

        return Product(
            id = this.id,
            name = this.name,
            code = this.code,
            groupId = this.group_id ?: "",
            brandId = this.brand_id ?: "",
            categoryId = this.category_id ?: "",
            subCategoryId = this.sub_category_id ?: "",
            active = this.active == 1,
            taxCode = this.tax_code,
            mrp = this.mrp,
            dp = this.dp,
            sellingPrice = this.selling_price,
            baseUnitId = this.base_unit,
            description = this.description ?: "",
            stockQuantity = this.stock_quantity,
            lowStockAlert = this.low_stock_alert,
            productType = this.product_type?.let { type ->
                try { ProductType.valueOf(type) } catch (_: Exception) { null }
            },
            serviceType = this.service_type?.let { type ->
                try { ServiceType.valueOf(type) } catch (_: Exception) { null }
            },
            hasVariants = this.has_variants == 1,
            variants = variants
        )
    }

    private fun ProductEntity.toProductListItem(
        categoryNames: Map<String, String>,
        brandNames: Map<String, String>,
    ): ProductListItem {
        return ProductListItem(
            id = this.id,
            name = this.name,
            code = this.code,
            mrp = this.mrp,
            sellingPrice = this.selling_price,
            categoryName = this.category_id?.let { categoryNames[it] },
            brandName = this.brand_id?.let { brandNames[it] },
            stockQuantity = this.stock_quantity,
            imageUrl = null, // TODO: Join with image table
            active = this.active == 1
        )
    }

    // ==================== Product Variant Methods ====================

    /**
     * Observe all variants for a product (reactive, offline-first)
     */
    fun observeProductVariants(productId: String): Flow<List<ProductVariant>> {
        return variantDao.observeProductVariants(productId)
            .map { entities -> entities.toDomainList() }
    }

    /**
     * Get variant by ID
     */
    suspend fun getVariantById(variantId: String): ProductVariant? {
        return variantDao.getVariantById(variantId)?.toDomain()
    }

    /**
     * Get variant by SKU
     */
    suspend fun getVariantBySku(sku: String): ProductVariant? {
        return variantDao.getVariantBySku(sku)?.toDomain()
    }

    /**
     * Create variant (database-first with background sync)
     */
    @OptIn(ExperimentalTime::class)
    suspend fun createVariant(variant: ProductVariant): Result<ProductVariant> {
        return try {
            require(variant.id.isNotBlank()) { "Variant ID must be set" }
            require(variant.sku.isNotBlank()) { "SKU is required" }

            val now = Clock.System.now()
            val variantWithTimestamps = variant.copy(
                createdAt = now.toString(),
                updatedAt = now.toString(),
                lastUpdated = now.toEpochMilliseconds(),
                synced = false
            )

            // 1. Save to database first (offline-first)
            val entity = variantWithTimestamps.toEntity()
            variantDao.insertVariant(entity)

            // 2. Update variant attributes for searchability
            updateVariantAttributes(variant.productId, variant)

            // TODO: Background sync to server when backend is ready

            Result.success(variantWithTimestamps)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.createVariant")
            Result.failure(e)
        }
    }

    /**
     * Update variant (database-first with background sync)
     */
    @OptIn(ExperimentalTime::class)
    suspend fun updateVariant(variant: ProductVariant): Result<ProductVariant> {
        return try {
            val now = Clock.System.now()
            val updatedVariant = variant.copy(
                updatedAt = now.toString(),
                lastUpdated = now.toEpochMilliseconds(),
                synced = false
            )

            // 1. Update database first
            variantDao.updateVariant(updatedVariant.toEntity())

            // 2. Update variant attributes
            updateVariantAttributes(variant.productId, variant)

            // TODO: Background sync to server when backend is ready

            Result.success(updatedVariant)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.updateVariant")
            Result.failure(e)
        }
    }

    /**
     * Delete variant (soft delete)
     */
    suspend fun deleteVariant(variantId: String): Result<Unit> {
        return try {
            variantDao.deleteVariant(variantId)
            // TODO: Background sync to server when backend is ready
            Result.success(Unit)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.deleteVariant")
            Result.failure(e)
        }
    }

    /**
     * Get total stock across all variants
     */
    suspend fun getTotalVariantStock(productId: String): Double {
        return variantDao.getTotalProductStock(productId)
    }

    /**
     * Get available attribute names for a product
     */
    suspend fun getAttributeNames(productId: String): List<String> {
        return attributeDao.getAttributeNames(productId)
    }

    /**
     * Get available values for a specific attribute
     */
    suspend fun getAttributeValues(productId: String, attributeName: String): List<String> {
        return attributeDao.getAttributeValues(productId, attributeName)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun updateVariantAttributes(productId: String, variant: ProductVariant) {
        try {
            val now = Clock.System.now().toString()
            val attributes = mutableListOf<VariantAttributeEntity>()

            variant.attribute1Name?.let { name ->
                variant.attribute1Value?.let { value ->
                    attributes.add(
                        VariantAttributeEntity(
                            productId = productId,
                            attributeName = name,
                            attributeValue = value,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }

            variant.attribute2Name?.let { name ->
                variant.attribute2Value?.let { value ->
                    attributes.add(
                        VariantAttributeEntity(
                            productId = productId,
                            attributeName = name,
                            attributeValue = value,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }

            variant.attribute3Name?.let { name ->
                variant.attribute3Value?.let { value ->
                    attributes.add(
                        VariantAttributeEntity(
                            productId = productId,
                            attributeName = name,
                            attributeValue = value,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }

            if (attributes.isNotEmpty()) {
                attributeDao.insertAttributes(attributes)
            }
        } catch (e: Exception) {
            ProductLogger.w("ProductRepository", "Failed to update variant attributes", e)
        }
    }
}