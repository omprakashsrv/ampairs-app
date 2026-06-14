package com.ampairs.product.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.domain.asDatabaseModel
import com.ampairs.product.domain.asProductApiModel
import com.ampairs.product.util.ProductLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all product ↔ server traffic. The repository is local-only; this delegate is the single
 * place that talks to [ProductApi] — bulk push of unsynced rows, batched pull (permanently
 * deleting server-DELETED rows), and backend event refresh.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PRODUCT)
class ProductSyncDelegate(
    private val productApi: ProductApi,
    private val productDao: ProductDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRODUCT

    // PRODUCT_CATALOG (groups/brands/categories/sub-categories) must be on the server before
    // products can be inserted due to FK constraints.
    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.PRODUCT_CATALOG)

    // For pulls, products reference catalog, unit and tax — pull those first so a product's
    // unit/tax/category references resolve locally.
    override val dependsOn: List<SyncEntity> =
        listOf(SyncEntity.PRODUCT_CATALOG, SyncEntity.UNIT, SyncEntity.TAX)

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { refreshProductFromServer(entityId) }.fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )

    private suspend fun pushPending(): Result<Int> = runCatching {
        val unsynced = productDao.unSyncedProducts()
        if (unsynced.isEmpty()) return@runCatching 0
        var pushed = 0
        var failed = 0
        var lastError: Throwable? = null
        // One failed batch must not abort the rest (reference pattern: CustomerSyncDelegate) —
        // remaining batches still push; failed rows stay synced=0 and retry next cycle.
        for (batch in unsynced.chunked(100)) {
            val apiModels = batch.map { it.asProductApiModel() }
            productApi.bulkUpdateProducts(apiModels)
                .onSuccess {
                    batch.forEach { entity -> productDao.insert(entity.copy(synced = 1)) }
                    pushed += batch.size
                }
                .onFailure { error ->
                    ProductLogger.w("ProductSyncDelegate", "Batch push failed", error)
                    failed += batch.size
                    lastError = error
                }
        }
        // Rule 2 (/offline-sync): nothing pushed + failures present must surface as failure,
        // not Success(0), so CentralSyncService marks the entity FAILED and retries on reconnect.
        if (pushed == 0 && failed > 0) {
            throw lastError ?: Exception("$failed product(s) failed to push — will retry on reconnect")
        }
        pushed
    }

    private suspend fun pull(): Result<Int> = runCatching {
        var page = 0
        var total = 0
        var hasNext: Boolean
        do {
            val pageResp = productApi.getProductsSync(
                lastSync = null,
                page = page,
                size = 100,
                sortBy = "updatedAt",
                sortDir = "ASC",
            ).getOrThrow()
            val batch = pageResp.content
            // Permanently delete locally anything the server reports as DELETED.
            batch.filter { it.status?.equals("DELETED", ignoreCase = true) == true }
                .forEach { productDao.deleteById(it.id) }
            // Upsert the rest.
            val toUpsert = batch.filter { it.status?.equals("DELETED", ignoreCase = true) != true }
            if (toUpsert.isNotEmpty()) productDao.insertAll(preserveLocalStock(toUpsert.asDatabaseModel()))
            total += batch.size
            hasNext = pageResp.hasNext
            page++
        } while (hasNext && total < 10000)
        total
    }

    /**
     * The product `/sync` contract does not carry stock (it lives in the inventory bounded context),
     * so a server pull would otherwise null out stock written locally from Tally. Keep the local
     * stock_quantity whenever the server copy doesn't provide one; the server value wins when present.
     */
    private suspend fun preserveLocalStock(entities: List<ProductEntity>): List<ProductEntity> {
        if (entities.isEmpty()) return entities
        val existing = productDao.productsByIds(entities.map { it.id }).associateBy { it.id }
        return entities.map { e ->
            if (e.stock_quantity == null) e.copy(stock_quantity = existing[e.id]?.stock_quantity) else e
        }
    }

    private suspend fun refreshProductFromServer(productId: String) {
        productApi.getProduct(productId)
            .onSuccess { model ->
                productDao.insertAll(preserveLocalStock(listOf(model).asDatabaseModel()))
                ProductLogger.i("ProductSyncDelegate", "✅ Refreshed product from server: $productId")
            }
            .onFailure { error ->
                ProductLogger.w("ProductSyncDelegate", "Product not found on server: $productId - ${error.message}")
            }
    }
}
