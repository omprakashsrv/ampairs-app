package com.ampairs.order.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.order.api.OrderApi
import com.ampairs.order.api.model.OrderApiModel
import com.ampairs.order.api.model.toApiModel
import com.ampairs.order.db.dao.OrderDao
import com.ampairs.order.db.dto.asDatabaseModel
import com.ampairs.order.db.dto.asItemDatabaseModel
import com.ampairs.order.domain.asDomainModel
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns ALL order ↔ server traffic (spec 010). The repository is local-only; this delegate is the
 * single place that talks to [OrderApi]. CentralSyncService invokes it on PENDING_PUSH (bulk push),
 * PENDING_PULL (batched pull), and backend WebSocket events.
 *
 * Endpoints target the `/v1/orders/sync` contract (backend phase B1); until that lands the calls
 * fail and orders stay local (offline-first by construction).
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.ORDER)
class OrderSyncDelegate(
    private val orderApi: OrderApi,
    private val orderDao: OrderDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.ORDER

    // Orders reference customers and products — push/pull those first. pushDependencies so the PUSH
    // (not just pull) sends parents before the order; dependsOn inherits this list.
    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.CUSTOMER, SyncEntity.PRODUCT)

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

    // --- Push -------------------------------------------------------------------------------

    /** Bulk push all locally unsynced active orders, in batches of 100. */
    private suspend fun pushPending(): Result<Int> {
        return try {
            val unsynced = orderDao.getUnsyncedOrders()
            if (unsynced.isEmpty()) return Result.success(0)

            // Build full api models (order + items) from the DB, reusing the existing mappers.
            val apiModels = unsynced.mapNotNull { e ->
                orderDao.getOrderById(e.id)?.asDomainModel()?.toApiModel()
            }
            if (apiModels.isEmpty()) return Result.success(0)

            var synced = 0
            var failed = 0
            for (batch in apiModels.chunked(100)) {
                try {
                    orderApi.bulkUpdateOrders(batch)
                    batch.forEach { orderDao.markAsSynced(it.id) }
                    synced += batch.size
                } catch (e: Exception) {
                    ErrorTracking.captureException(e, "OrderSyncDelegate.pushPending")
                    failed += batch.size
                }
            }

            if (synced == 0 && failed > 0) {
                Result.failure(Exception("$failed order(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(synced)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Pull -------------------------------------------------------------------------------

    /**
     * Batched incremental pull. Local unsynced edits win; server rows marked deleted/inactive are
     * removed locally; everything else is upserted as synced. Advances the ISO checkpoint.
     */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.ORDER) ?: ""
            var total = 0
            var page = 0
            var maxTime = ""

            do {
                val pageResponse = orderApi.getOrdersSync(lastSync, page, batchSize, "updatedAt", "ASC")
                val content = pageResponse.content
                if (content.isNotEmpty()) {
                    val toUpsert = mutableListOf<OrderApiModel>()
                    for (api in content) {
                        val existing = orderDao.selectById(api.id)
                        when {
                            existing != null && existing.synced == 0L -> { /* local unsynced wins */ }
                            api.softDeleted || !api.active -> orderDao.deleteById(api.id)
                            else -> toUpsert += api
                        }
                    }
                    if (toUpsert.isNotEmpty()) {
                        orderDao.updateOrders(toUpsert.asDatabaseModel(), toUpsert.asItemDatabaseModel())
                    }
                    val batchMax = content
                        .mapNotNull { it.updatedAt?.takeIf { s -> s.isNotBlank() } }
                        .maxOrNull() ?: ""
                    if (batchMax > maxTime) maxTime = batchMax
                    total += content.size
                }
                page++
            } while (pageResponse.hasNext && total < 10000)

            if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.ORDER, maxTime)
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
