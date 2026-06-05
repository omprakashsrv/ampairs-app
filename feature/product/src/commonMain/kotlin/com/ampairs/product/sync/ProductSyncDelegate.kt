package com.ampairs.product.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.db.dao.ProductDao
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
        for (batch in unsynced.chunked(100)) {
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

    private suspend fun pull(): Result<Int> = runCatching {
        var page = 0
        var total = 0
        var hasNext: Boolean
        do {
            val pageResp = productApi.getProductsSync(lastSync = null, page = page, size = 100).getOrThrow()
            val batch = pageResp.content
            // Permanently delete locally anything the server reports as DELETED.
            batch.filter { it.status?.equals("DELETED", ignoreCase = true) == true }
                .forEach { productDao.deleteById(it.id) }
            // Upsert the rest.
            val toUpsert = batch.filter { it.status?.equals("DELETED", ignoreCase = true) != true }
            if (toUpsert.isNotEmpty()) productDao.insertAll(toUpsert.asDatabaseModel())
            total += batch.size
            hasNext = pageResp.hasNext
            page++
        } while (hasNext && total < 10000)
        total
    }

    private suspend fun refreshProductFromServer(productId: String) {
        productApi.getProduct(productId)
            .onSuccess { model ->
                productDao.insertAll(listOf(model).asDatabaseModel())
                ProductLogger.i("ProductSyncDelegate", "✅ Refreshed product from server: $productId")
            }
            .onFailure { error ->
                ProductLogger.w("ProductSyncDelegate", "Product not found on server: $productId - ${error.message}")
            }
    }
}
