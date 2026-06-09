package com.ampairs.customer.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.api.CustomerGroupApi
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.toCustomerGroup
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.util.CustomerLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all customer-group ↔ server traffic. The repository is local-only (plus the import
 * feature); this delegate is the single place that talks to [CustomerGroupApi] for sync.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CUSTOMER_GROUP)
class CustomerGroupSyncDelegate(
    private val customerGroupApi: CustomerGroupApi,
    private val customerGroupDao: CustomerGroupDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CUSTOMER_GROUP

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

    /**
     * Bulk push all locally unsynced rows through the unified /sync endpoint. Soft-deleted rows
     * (active = false) are sent IN-BAND in the same bulk body so the server can mark them DELETED;
     * there is no separate per-row DELETE call. After a successful push, soft-deleted rows are
     * hard-deleted locally and active rows are marked synced = true.
     */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = customerGroupDao.getUnsyncedCustomerGroups()
            if (unsynced.isEmpty()) return Result.success(0)

            var syncedCount = 0
            var failedCount = 0

            for (batch in unsynced.chunked(100)) {
                // Active rows derive a groupCode when missing; soft-deleted rows are included as-is
                // so the delete propagates server-side in the same bulk body.
                val groups = batch.map { entity ->
                    entity.toCustomerGroup().let { group ->
                        if (entity.active && group.groupCode.isNullOrBlank()) {
                            val derived = group.name
                                .filter { it.isLetterOrDigit() || it == ' ' }
                                .trim().replace(' ', '_').uppercase().take(20)
                                .ifBlank { group.uid.takeLast(8).uppercase() }
                            group.copy(groupCode = derived)
                        } else group
                    }
                }
                customerGroupApi.bulkUpsertGroups(groups)
                    .onSuccess {
                        // Push succeeded — reconcile each row locally.
                        batch.forEach { entity ->
                            if (!entity.active) {
                                customerGroupDao.hardDeleteCustomerGroup(entity.id)
                            } else {
                                customerGroupDao.insertCustomerGroup(entity.copy(synced = true))
                            }
                        }
                        syncedCount += batch.size
                    }
                    .onFailure { e ->
                        CustomerLogger.w("CustomerGroupSyncDelegate", "Batch upsert failed", e)
                        failedCount += batch.size
                    }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount customer group(s) failed to push"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupSyncDelegate", "Push failed", e)
            Result.failure(e)
        }
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            var totalSynced = 0
            var currentPage = 0

            do {
                val pageResponse = customerGroupApi.getCustomerGroups(currentPage, batchSize)
                if (pageResponse.error != null) throw Exception(pageResponse.error?.message ?: "Network error")
                val batchGroups = pageResponse.data?.content ?: emptyList()

                // Reconcile: local unsynced wins; server-removed (active=false) is permanently
                // deleted from the local DB; otherwise upsert.
                val groupsToInsert = batchGroups.mapNotNull { serverGroup ->
                    val existing = customerGroupDao.getCustomerGroupById(serverGroup.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !serverGroup.active -> {
                            customerGroupDao.hardDeleteCustomerGroup(serverGroup.uid)
                            null
                        }
                        else -> serverGroup.toEntity().copy(synced = true)
                    }
                }
                if (groupsToInsert.isNotEmpty()) customerGroupDao.insertCustomerGroups(groupsToInsert)
                totalSynced += batchGroups.size

                currentPage++
            } while (batchGroups.size == batchSize && totalSynced < 10000)

            Result.success(totalSynced)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupSyncDelegate", "Pull failed", e)
            Result.failure(e)
        }
    }
}
