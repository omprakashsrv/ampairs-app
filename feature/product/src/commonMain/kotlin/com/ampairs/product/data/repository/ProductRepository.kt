package com.ampairs.product.data.repository

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.EventType
import com.ampairs.product.util.ProductLogger
import com.ampairs.common.cache.CacheCleanable
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductVariantDao
import com.ampairs.product.db.dao.VariantAttributeDao
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.entity.VariantAttributeEntity
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductListItem
import com.ampairs.product.domain.ProductSummary
import com.ampairs.product.domain.ProductType
import com.ampairs.product.domain.ProductVariant
import com.ampairs.product.domain.ServiceType
import com.ampairs.product.domain.asDomainModel
import com.ampairs.product.domain.asDatabaseModel
import com.ampairs.product.domain.asProductApiModel
import com.ampairs.product.domain.toEntity
import com.ampairs.product.domain.toDomain
import com.ampairs.product.domain.toDomainList
import com.ampairs.product.domain.toSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository for product data following Store5 offline-first pattern
 * Similar to CustomerRepository implementation
 */
@Inject
class ProductRepository(
    private val productApi: ProductApi,
    private val productDao: ProductDao,
    private val variantDao: ProductVariantDao,
    private val attributeDao: VariantAttributeDao
) : ProductDataService, CacheCleanable {

    private suspend fun handleProductEvent(eventType: EventType, productId: String) {
        when (eventType) {
            EventType.PRODUCT_CREATED,
            EventType.PRODUCT_UPDATED,
            EventType.PRODUCT_STOCK_CHANGED -> refreshProductFromServer(productId)
            EventType.PRODUCT_DELETED -> productDao.deleteById(productId)
            else -> Unit
        }
    }

    /**
     * Refresh a single product from server (called when event received from another device).
     * Updates local Room database which automatically triggers Flow updates.
     */
    private suspend fun refreshProductFromServer(productId: String) {
        try {
            val result = productApi.getProduct(productId)

            result.onSuccess { productApiModel ->
                // Convert API model to domain, then to entity
                val product = Product(
                    id = productApiModel.id,
                    name = productApiModel.name,
                    code = productApiModel.code,
                    groupId = productApiModel.groupId ?: "",
                    brandId = productApiModel.brandId ?: "",
                    categoryId = productApiModel.categoryId ?: "",
                    subCategoryId = productApiModel.subCategoryId ?: "",
                    active = productApiModel.active,
                    taxCode = productApiModel.taxCode,
                    mrp = productApiModel.mrp,
                    dp = productApiModel.dp,
                    sellingPrice = productApiModel.sellingPrice,
                    baseUnitId = productApiModel.baseUnitId
                )

                // Update Room database - this automatically triggers Flow updates!
                productDao.insert(product.toEntity())

                ProductLogger.i("ProductRepository", "✅ Refreshed product from server: $productId")
            }.onFailure { error ->
                ProductLogger.w("ProductRepository", "Product not found on server: $productId - ${error.message}")
            }
        } catch (e: Exception) {
            ProductLogger.w("ProductRepository", "Failed to refresh product $productId: ${e.message}")
            // Graceful degradation - UI continues showing cached data
        }
    }

    fun observeProducts(): Flow<List<ProductListItem>> =
        productDao.observeAllProducts().map { entities -> entities.map { it.toProductListItem() } }

    fun searchProducts(query: String): Flow<List<ProductListItem>> =
        if (query.isBlank()) {
            productDao.observeAllProducts().map { entities -> entities.map { it.toProductListItem() } }
        } else {
            productDao.observeProductsByName(query).map { entities -> entities.map { it.toProductListItem() } }
        }

    fun observeProduct(productId: String): Flow<Product?> =
        productDao.observeProductById(productId).map { entity -> entity?.toDomainProduct() }

    /**
     * Get products by category
     */
    suspend fun getProductsByCategory(categoryIds: List<String>): List<ProductListItem> {
        val products = productDao.productsByCategoryIds(categoryIds)
        return products.map { it.toProductListItem() }
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

    override suspend fun clearCache() { productDao.deleteAll() }

    suspend fun syncProducts(): Result<Int> = runCatching {
        val products = productApi.getProducts().getOrThrow()
        productDao.insertAll(products.asDatabaseModel())
        products.size
    }

    suspend fun createProduct(product: Product): Result<Product> {
        return try {
            val productWithId = if (product.id.isBlank()) {
                product.copy(id = generateLocalId())
            } else product

            productDao.insert(productWithId.toEntity())
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
            Result.success(product)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.updateProduct")
            Result.failure(e)
        }
    }

    /**
     * Delete a product
     */
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            productDao.deleteById(productId)
            Result.success(Unit)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.deleteProduct")
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun generateLocalId(): String {
        return "local_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }

    suspend fun getProductCount(): Int {
        return productDao.countProducts()
    }

    suspend fun getUnSyncedProducts(): List<Product> {
        return productDao.unSyncedProducts().map { it.toDomainProduct() }
    }

    suspend fun pullFromServer(): Result<Int> = syncProducts()

    // Push unsynced local changes first, then pull server updates — mirrors CustomerRepository.syncCustomers()
    suspend fun fullSync(): Result<Int> {
        val pushResult = pushPendingToServer()
        val pullResult = syncProducts()
        return if (pullResult.isFailure) pullResult
        else Result.success((pushResult.getOrElse { 0 }) + (pullResult.getOrElse { 0 }))
    }

    suspend fun pushPendingToServer(): Result<Int> = runCatching {
        val unsynced = productDao.unSyncedProducts()
        if (unsynced.isEmpty()) return@runCatching 0
        var pushed = 0
        for (batch in unsynced.chunked(10)) {
            val apiModels = batch.map { it.asProductApiModel() }
            productApi.bulkUpdateProducts(apiModels)
                .onSuccess {
                    batch.forEach { entity -> productDao.insert(entity.copy(synced = 1)) }
                    pushed += batch.size
                }
                .onFailure { throw it }
        }
        pushed
    }

    suspend fun handleExternalEvent(productId: String, eventType: String) {
        refreshProductFromServer(productId)
    }

    // Extension functions for data conversion
    @OptIn(ExperimentalTime::class)
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

    private fun ProductEntity.toProductListItem(): ProductListItem {
        return ProductListItem(
            id = this.id,
            name = this.name,
            code = this.code,
            mrp = this.mrp,
            sellingPrice = this.selling_price,
            categoryName = this.category_id, // TODO: Join with category table for actual name
            brandName = this.brand_id, // TODO: Join with brand table for actual name
            stockQuantity = null, // TODO: Add stock management
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