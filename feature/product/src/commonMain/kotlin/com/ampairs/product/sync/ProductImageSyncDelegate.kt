package com.ampairs.product.sync

import com.ampairs.common.di.AppScope
import com.ampairs.product.data.repository.ProductImageRepository
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.PRODUCT_IMAGE)
class ProductImageSyncDelegate(
    private val repository: ProductImageRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRODUCT_IMAGE

    override suspend fun pullFromServer(): SyncResult =
        repository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        repository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        repository.syncProductImages(entityId).fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )
}
