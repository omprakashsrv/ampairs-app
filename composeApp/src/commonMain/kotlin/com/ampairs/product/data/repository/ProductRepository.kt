package com.ampairs.product.data.repository

import com.ampairs.event.EventManager
import com.ampairs.event.domain.EventType
import com.ampairs.event.util.EventLogger
import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductListItem
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
@OptIn(ExperimentalTime::class)
class ProductRepository(
    private val productApi: ProductApi,
    private val productDao: ProductDao
) {
    // Event listener job for cleanup
    private var eventListenerJob: Job? = null

    /**
     * Set up real-time event listener for product updates from other devices.
     * Call this after workspace is selected and EventManager is available.
     *
     * @param eventManager The EventManager instance for the current workspace
     */
    fun setupEventListener(eventManager: EventManager) {
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
            Result.failure(e)
        }
    }

    /**
     * Update an existing product
     */
    suspend fun updateProduct(product: Product): Result<Product> {
        return try {
            productDao.update(product.toEntity())
            Result.success(product)
        } catch (e: Exception) {
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
            base_unit = this.baseUnitId,
            last_updated = Clock.System.now().toEpochMilliseconds(),
            created_at = Clock.System.now().toString(),
            updated_at = Clock.System.now().toString(),
            synced = 0 // Mark as unsynced initially
        )
    }

    private fun ProductEntity.toDomainProduct(): Product {
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
            baseUnitId = this.base_unit
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
}