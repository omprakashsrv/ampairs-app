package com.ampairs.customer.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.toCustomerType
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.util.CustomerLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all customer-type ↔ server traffic. The repository is local-only (plus the import
 * feature); this delegate is the single place that talks to [CustomerTypeApi] for sync.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CUSTOMER_TYPE)
class CustomerTypeSyncDelegate(
    private val customerTypeApi: CustomerTypeApi,
    private val customerTypeDao: CustomerTypeDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CUSTOMER_TYPE

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
        pullFromServer()

    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = customerTypeDao.getUnsyncedCustomerTypes()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0

            for (entity in unsynced.filter { !it.active }) {
                try {
                    customerTypeApi.deleteCustomerType(entity.id)
                    customerTypeDao.hardDeleteCustomerType(entity.id)
                    syncedCount++
                } catch (e: Exception) {
                    CustomerLogger.w("CustomerTypeSyncDelegate", "Failed to delete type ${entity.id}", e)
                    failedCount++
                }
            }

            val activeUnsynced = unsynced.filter { it.active }
            for (batch in activeUnsynced.chunked(100)) {
                val types = batch.map { it.toCustomerType() }
                customerTypeApi.bulkUpsertTypes(types)
                    .onSuccess {
                        batch.forEach { entity -> customerTypeDao.insertCustomerType(entity.copy(synced = true)) }
                        syncedCount += batch.size
                    }
                    .onFailure { e ->
                        CustomerLogger.w("CustomerTypeSyncDelegate", "Batch upsert failed", e)
                        failedCount += batch.size
                    }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount customer type(s) failed to push"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            var totalSynced = 0
            var currentPage = 0

            do {
                val pageResponse = customerTypeApi.getCustomerTypes(currentPage, batchSize)
                if (pageResponse.error != null) throw Exception(pageResponse.error?.message ?: "Network error")
                val batchTypes = pageResponse.data?.content ?: emptyList()

                // Reconcile: local unsynced wins; server-removed (active=false) is permanently
                // deleted from the local DB; otherwise upsert.
                val typesToInsert = batchTypes.mapNotNull { serverType ->
                    val existing = customerTypeDao.getCustomerTypeById(serverType.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !serverType.active -> {
                            customerTypeDao.hardDeleteCustomerType(serverType.uid)
                            null
                        }
                        else -> serverType.toEntity().copy(synced = true)
                    }
                }
                if (typesToInsert.isNotEmpty()) customerTypeDao.insertCustomerTypes(typesToInsert)
                totalSynced += batchTypes.size

                currentPage++
            } while (batchTypes.size == batchSize && totalSynced < 10000)

            Result.success(totalSynced)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }
}
