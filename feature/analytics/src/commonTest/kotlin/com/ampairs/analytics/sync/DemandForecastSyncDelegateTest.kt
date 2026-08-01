package com.ampairs.analytics.sync

import com.ampairs.analytics.data.api.AnalyticsApi
import com.ampairs.analytics.data.api.DemandForecastResponse
import com.ampairs.analytics.data.api.KpiResponseDto
import com.ampairs.analytics.data.api.TrendPointDto
import com.ampairs.analytics.data.db.dao.DemandForecastDao
import com.ampairs.analytics.data.db.entity.DemandForecastEntity
import com.ampairs.common.model.PageResponse
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncPersistStatus
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pull semantics of [DemandForecastSyncDelegate] over hand-rolled fakes (feature 022, T036): the
 * batched pull upserts active rows, permanently deletes rows the server reports inactive, advances the
 * incremental `updatedAt` checkpoint, and never pushes (server-generated). Mirrors FormSyncDelegateTest.
 */
class DemandForecastSyncDelegateTest {

    private class FakeDemandForecastDao : DemandForecastDao {
        val rows = linkedMapOf<String, DemandForecastEntity>()
        override fun observeAll(): Flow<List<DemandForecastEntity>> = MutableStateFlow(rows.values.toList())
        override fun observeForProduct(productId: String): Flow<List<DemandForecastEntity>> =
            MutableStateFlow(rows.values.filter { it.productId == productId })
        override suspend fun getByUid(uid: String): DemandForecastEntity? = rows[uid]
        override suspend fun upsert(forecast: DemandForecastEntity) { rows[forecast.uid] = forecast }
        override suspend fun upsertAll(forecasts: List<DemandForecastEntity>) {
            forecasts.forEach { rows[it.uid] = it }
        }
        override suspend fun deleteByUid(uid: String) { rows.remove(uid) }
        override suspend fun maxUpdatedAt(): String? = rows.values.mapNotNull { it.updatedAt }.maxOrNull()
        override suspend fun clear() { rows.clear() }
        override suspend fun latestPerProduct(limit: Int): List<DemandForecastEntity> = rows.values.take(limit)
    }

    private class FakeSyncStateDao : SyncStateDao {
        var lastSyncedAtIso: String? = null
        override fun observeAll(): Flow<List<SyncStateEntity>> = MutableStateFlow(emptyList())
        override fun observe(entity: SyncEntity): Flow<SyncStateEntity?> = MutableStateFlow(null)
        override suspend fun getAll(): List<SyncStateEntity> = emptyList()
        override suspend fun getPending(): List<SyncStateEntity> = emptyList()
        override suspend fun upsert(state: SyncStateEntity) {}
        override suspend fun upsertStatus(
            entity: SyncEntity,
            status: SyncPersistStatus,
            lastSyncedAt: Long?,
            pendingCount: Int,
            errorMessage: String?,
            now: Long,
        ) {}
        override suspend fun getLastSyncedAtIso(entity: SyncEntity): String? = lastSyncedAtIso
        override suspend fun setLastSyncedAtIso(entity: SyncEntity, iso: String) { lastSyncedAtIso = iso }
        override suspend fun markPendingPush(entity: SyncEntity, now: Long) {}
        override suspend fun deleteAll() {}
    }

    private class FakeAnalyticsApi(private val firstPage: PageResponse<DemandForecastResponse>) : AnalyticsApi {
        override suspend fun getForecastsSync(
            lastSync: String,
            page: Int,
            size: Int,
            sortBy: String,
            sortDir: String,
        ): PageResponse<DemandForecastResponse> = if (page == 0) firstPage else emptyPage()

        override suspend fun getKpis(fromDate: String, toDate: String, period: String, metricGroup: String) =
            KpiResponseDto()

        override suspend fun getTrend(fromDate: String, toDate: String, period: String, metricId: String) =
            emptyList<TrendPointDto>()

        private fun emptyPage() = PageResponse<DemandForecastResponse>(
            content = emptyList(), pageNumber = 0, pageSize = 100, totalPages = 0, totalElements = 0L,
            hasNext = false, hasPrevious = false, first = true, last = true,
        )
    }

    private fun response(uid: String, productId: String, updatedAt: String, active: Boolean) =
        DemandForecastResponse(
            uid = uid, productId = productId, periodStart = "2026-07-01", horizon = 7,
            meanQty = 14.0, stdDevQty = 2.0, method = "HOLT_WINTERS", confidence = "HIGH",
            generatedAt = updatedAt, updatedAt = updatedAt, active = active,
        )

    private fun page(vararg rows: DemandForecastResponse) = PageResponse(
        content = rows.toList(), pageNumber = 0, pageSize = 100, totalPages = 1,
        totalElements = rows.size.toLong(), hasNext = false, hasPrevious = false, first = true, last = true,
    )

    @Test
    fun `pull upserts active rows, deletes inactive, advances the checkpoint`() = runTest {
        val dao = FakeDemandForecastDao().apply {
            // A previously-synced row the server now reports retired.
            rows["F2"] = DemandForecastEntity(
                uid = "F2", productId = "PRD2", periodStart = "2026-06-01", horizon = 7,
                meanQty = 5.0, stdDevQty = 1.0, method = "MOVING_AVG", confidence = "LOW",
                updatedAt = "2026-06-01T00:00:00Z",
            )
        }
        val syncState = FakeSyncStateDao()
        val api = FakeAnalyticsApi(
            page(
                response("F1", "PRD1", "2026-07-01T02:30:00Z", active = true),
                response("F2", "PRD2", "2026-07-02T02:30:00Z", active = false),
            ),
        )
        val delegate = DemandForecastSyncDelegate(api, dao, syncState)

        val result = delegate.pullFromServer()

        assertTrue(result is SyncResult.Success)
        assertEquals(2, (result as SyncResult.Success).count)
        assertTrue("F1" in dao.rows, "active row upserted")
        assertFalse("F2" in dao.rows, "inactive row hard-deleted")
        assertEquals("2026-07-02T02:30:00Z", syncState.lastSyncedAtIso, "checkpoint = max updatedAt")
    }

    @Test
    fun `push is a no-op (forecasts are server-generated)`() = runTest {
        val delegate = DemandForecastSyncDelegate(FakeAnalyticsApi(page()), FakeDemandForecastDao(), FakeSyncStateDao())
        val result = delegate.pushPendingToServer()
        assertTrue(result is SyncResult.Success)
        assertEquals(0, (result as SyncResult.Success).count)
    }
}
