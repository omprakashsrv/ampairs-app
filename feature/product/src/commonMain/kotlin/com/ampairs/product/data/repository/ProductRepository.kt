package com.ampairs.product.data.repository

import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.common.event.IEventManager
import com.ampairs.common.event.EventType
import com.ampairs.common.event.EventLogger
import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductVariantDao
import com.ampairs.product.db.dao.VariantAttributeDao
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.db.entity.VariantAttributeEntity
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductListItem
import com.ampairs.product.domain.ProductVariant
import com.ampairs.product.domain.toEntity
import com.ampairs.product.domain.toDomain
import com.ampairs.product.domain.toDomainList
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository for product data following Store5 offline-first pattern
 * Similar to CustomerRepository implementation
 */
@Inject
@OptIn(ExperimentalTime::class)
class ProductRepository(
    private val productApi: ProductApi,
    private val productDao: ProductDao,
    private val variantDao: ProductVariantDao,
    private val attributeDao: VariantAttributeDao
) {
    // Event listener job for cleanup
    private var eventListenerJob: Job? = null

    /**
     * Set up real-time event listener for product updates from other devices.
     * Call this after workspace is selected and EventManager is available.
     *
     * @param eventManager The EventManager instance for the current workspace
     */
    fun setupEventListener(eventManager: IEventManager) {
        // Cancel existing listener if any
        eventListenerJob?.cancel()

        // Set up new listener
        eventListenerJob = CoroutineScope(Dispatchers.Default).launch {
            eventManager.events
                .filter { it.isForEntityType("product") }
                .collect { event ->
                    handleProductEvent(event.eventType, event.entityId)
                }
        }
        EventLogger.i("ProductRepository", "Real-time event listener initialized for product module")
    }

    /**
     * Stop listening to real-time events (e.g., when switching workspaces)
     */
    fun stopEventListener() {
        eventListenerJob?.cancel()
        eventListenerJob = null
        EventLogger.i("ProductRepository", "Real-time event listener stopped")
    }

    /**
     * Handle incoming product events from other devices.
     * Updates local database to reflect changes made on other devices.
     */
    private suspend fun handleProductEvent(eventType: EventType, productId: String) {
        EventLogger.i("ProductRepository", "📨 Received event: $eventType for product: $productId")

        when (eventType) {
            EventType.PRODUCT_CREATED,
            EventType.PRODUCT_UPDATED,
            EventType.PRODUCT_STOCK_CHANGED -> {
                // Fetch fresh data from server and update local database
                refreshProductFromServer(productId)
            }

            EventType.PRODUCT_DELETED -> {
                // Delete from local database
                productDao.deleteById(productId)
                EventLogger.i("ProductRepository", "🗑️ Deleted product: $productId")
            }

            else -> {
                // Ignore other event types
            }
        }
    }

    /**
     * Refresh a single product from server (called when event received from another device).
     * Updates local Room database which automatically triggers Flow updates.
     */
    private suspend fun refreshProductFromServer(productId: String) {
        try {
            // Get current workspace ID from context
            val workspaceId = WorkspaceContextManager.getInstance().currentWorkspace.value?.id
            if (workspaceId == null) {
                EventLogger.w("ProductRepository", "No workspace context available")
                return
            }

            // Fetch latest product data from server
            val result = productApi.getProduct(workspaceId, productId)

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

                EventLogger.i("ProductRepository", "✅ Refreshed product from server: $productId")
            }.onFailure { error ->
                EventLogger.w("ProductRepository", "Product not found on server: $productId - ${error.message}")
            }
        } catch (e: Exception) {
            EventLogger.w("ProductRepository", "Failed to refresh product $productId: ${e.message}")
            // Graceful degradation - UI continues showing cached data
        }
    }

    /**
     * Get all products as Flow
     */
    fun observeProducts(): Flow<List<ProductListItem>> {
        return flow {
            val products = productDao.getProducts()
            emit(products.map { it.toProductListItem() })
        }
    }

    /**
     * Search products by name or code
     */
    fun searchProducts(query: String): Flow<List<ProductListItem>> {
        return flow {
            val products = productDao.getProductsByName(query)
            emit(products.map { it.toProductListItem() })
        }
    }

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

    /**
     * Create a new product
     */
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

    private fun generateLocalId(): String {
        return "local_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }

    suspend fun getProductCount(): Int {
        return productDao.countProducts()
    }

    suspend fun getUnSyncedProducts(): List<Product> {
        return productDao.unSyncedProducts().map { it.toDomainProduct() }
    }

    // Extension functions for data conversion
    private fun Product.toEntity(): ProductEntity {
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
            last_updated = Clock.System.now().toEpochMilliseconds(),
            created_at = Clock.System.now().toString(),
            updated_at = Clock.System.now().toString(),
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
                try { com.ampairs.product.domain.ProductType.valueOf(type) } catch (e: Exception) { null }
            },
            serviceType = this.service_type?.let { type ->
                try { com.ampairs.product.domain.ServiceType.valueOf(type) } catch (e: Exception) { null }
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

            // 3. Background sync to server (graceful failure)
            try {
                // TODO: API call when backend is ready
                // val serverVariant = productApi.createVariant(variantWithTimestamps)
                // variantDao.insertVariant(serverVariant.toEntity().copy(synced = 1))
            } catch (e: Exception) {
                EventLogger.w("ProductRepository", "Variant sync failed, will retry later", e)
            }

            Result.success(variantWithTimestamps)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "ProductRepository.createVariant")
            Result.failure(e)
        }
    }

    /**
     * Update variant (database-first with background sync)
     */
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

            // 3. Background sync to server
            try {
                // TODO: API call when backend is ready
                // val serverVariant = productApi.updateVariant(updatedVariant)
                // variantDao.insertVariant(serverVariant.toEntity().copy(synced = 1))
            } catch (e: Exception) {
                EventLogger.w("ProductRepository", "Variant sync failed, will retry later", e)
            }

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

            // Background sync
            try {
                // TODO: API call when backend is ready
                // productApi.deleteVariant(variantId)
            } catch (e: Exception) {
                EventLogger.w("ProductRepository", "Variant delete sync failed", e)
            }

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

    /**
     * Update searchable variant attributes
     */
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
            EventLogger.w("ProductRepository", "Failed to update variant attributes", e)
        }
    }
}