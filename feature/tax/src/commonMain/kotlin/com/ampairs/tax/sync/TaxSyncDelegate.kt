package com.ampairs.tax.sync

import com.ampairs.common.di.AppScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.tax.data.repository.TaxCodeRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.TAX)
class TaxSyncDelegate(
    private val taxCodeRepository: TaxCodeRepository,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.TAX

    override suspend fun pullFromServer(): SyncResult =
        taxCodeRepository.syncWorkspaceTaxCodes().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        taxCodeRepository.syncUnsyncedChanges().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        taxCodeRepository.syncWorkspaceTaxCodes().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )
}
