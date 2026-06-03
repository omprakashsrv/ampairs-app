package com.ampairs.product.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PRODUCT)
class ProductSyncDelegate(
    private val productRepository: ProductRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRODUCT

    // PRODUCT_CATALOG (groups/brands/categories/sub-categories) must be on the server before
    // products can be inserted due to FK constraints.
    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.PRODUCT_CATALOG)

    override suspend fun pullFromServer(): SyncResult =
        productRepository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        productRepository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { productRepository.handleExternalEvent(entityId, eventType) }.fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )
}
