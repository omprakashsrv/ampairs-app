@file:OptIn(ExperimentalTime::class)

package com.ampairs.analytics.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.analytics.data.api.AnalyticsApi
import com.ampairs.analytics.data.query.DashboardReadFacade
import com.ampairs.analytics.domain.DashboardCoverage
import com.ampairs.analytics.domain.DashboardData
import com.ampairs.analytics.domain.DashboardPeriod
import com.ampairs.analytics.domain.DeepHistorySlice
import com.ampairs.analytics.domain.SalesTrendPoint
import com.ampairs.analytics.domain.mergePriorSlice
import com.ampairs.common.agent.DateRange
import com.ampairs.common.agent.ReportPeriod
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class DashboardUiState(
    val period: DashboardPeriod = DashboardPeriod.THIS_MONTH,
    val data: DashboardData = DashboardData(),
    val coverage: DashboardCoverage = DashboardCoverage.Full,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastSyncedAt: Long? = null,
    val error: String? = null,
)

/**
 * Analytics dashboard ViewModel (feature 022, T031). Reads the offline-first per-module aggregates
 * through [DashboardReadFacade] for the selected [DashboardPeriod], recomputing on period change.
 *
 * Period boundaries are computed in the **business** time zone (FR: business-zone bucketing). The VM
 * can't read `LocalAppLocale` (a CompositionLocal), so the screen pushes the zone id in via
 * [setLocale]; it defaults to UTC until then.
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class DashboardViewModel(
    private val facade: DashboardReadFacade,
    private val syncService: CentralSyncService,
    private val analyticsApi: AnalyticsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var timeZoneId: String = "UTC"

    init {
        // Spinner + freshness stamp driven by invoice sync (the dominant KPI source).
        syncService.observeEntity(SyncEntity.INVOICE)
            .onEach { st ->
                _uiState.update {
                    it.copy(
                        isRefreshing = st?.status is SyncStatus.Syncing,
                        lastSyncedAt = st?.lastSyncedAt ?: it.lastSyncedAt,
                    )
                }
            }
            .launchIn(viewModelScope)

        // Re-read local aggregates whenever a sync finishes (data may have changed).
        syncService.observeEntity(SyncEntity.INVOICE)
            .map { it?.status is SyncStatus.Success }
            .distinctUntilChanged()
            .onEach { done -> if (done) reload() }
            .launchIn(viewModelScope)

        // Freshen the local mirrors on open.
        SYNCED_ENTITIES.forEach { syncService.emit(SyncEvent.TriggerPull(it)) }
        reload()
    }

    /** Called by the screen with the workspace business time zone (`AppLocale.timeZoneId`). */
    fun setLocale(timeZoneId: String) {
        if (this.timeZoneId == timeZoneId) return
        this.timeZoneId = timeZoneId
        reload()
    }

    fun selectPeriod(period: DashboardPeriod) {
        if (_uiState.value.period == period) return
        _uiState.update { it.copy(period = period) }
        reload()
    }

    fun refresh() {
        SYNCED_ENTITIES.forEach { syncService.emit(SyncEvent.TriggerFullSync(it)) }
        reload()
    }

    private fun reload() {
        val period = _uiState.value.period
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val tz = runCatching { TimeZone.of(timeZoneId) }.getOrDefault(TimeZone.UTC)
                val now = Clock.System.now()
                val today = now.toLocalDateTime(tz).date
                // ALL_TIME → null range from ReportPeriod → widen to [epoch, tomorrow).
                val range = ReportPeriod.parse(period.keyword, tz, now)
                    ?: DateRange(
                        startInclusive = Instant.fromEpochMilliseconds(0L),
                        endExclusive = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz),
                    )
                val local = facade.load(range, today, tz)
                applyCoverage(local, period, range, tz)
            }.onSuccess { (data, coverage) ->
                _uiState.update { it.copy(data = data, coverage = coverage, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Sync-window boundary handling (T030a / FR-011). If the selected period starts before the
     * earliest locally-synced date, fetch the earlier remainder from the backend deep-history reads
     * and merge it (ranges are disjoint → additive). If that fetch fails (offline / error), render
     * the local aggregates with a reduced-coverage badge instead of silently undercounting. ALL_TIME
     * and periods fully within the local window need no server round-trip.
     */
    private suspend fun applyCoverage(
        local: DashboardData,
        period: DashboardPeriod,
        range: DateRange,
        tz: TimeZone,
    ): Pair<DashboardData, DashboardCoverage> {
        if (period == DashboardPeriod.ALL_TIME) return local to DashboardCoverage.Full
        val earliest = facade.earliestLocalBusinessDate() ?: return local to DashboardCoverage.Full
        val periodStart = range.startInclusive.toLocalDateTime(tz).date
        if (periodStart >= earliest) return local to DashboardCoverage.Full
        return try {
            val slice = fetchDeepHistory(periodStart, earliest.minus(1, DateTimeUnit.DAY))
            local.mergePriorSlice(slice) to DashboardCoverage.Full
        } catch (e: Exception) {
            local to DashboardCoverage.Reduced(earliest)
        }
    }

    /** Fetch the additive KPI + trend totals for `[from, to]` (inclusive) from the backend (T030). */
    private suspend fun fetchDeepHistory(from: LocalDate, to: LocalDate): DeepHistorySlice {
        val fromS = from.toString()
        val toS = to.toString()
        val sales = analyticsApi.getKpis(fromS, toS, "MONTH", "SALES").values.associate { it.metricId to it.value }
        val collections = analyticsApi.getKpis(fromS, toS, "MONTH", "COLLECTIONS").values.associate { it.metricId to it.value }
        // Trend is best-effort — a rejected day-grain doesn't fail the (more important) KPI merge.
        val trend = runCatching {
            analyticsApi.getTrend(fromS, toS, "DAY", "sales.gross").map { SalesTrendPoint(it.bucketStart, it.value) }
        }.getOrDefault(emptyList())
        return DeepHistorySlice(
            grossSales = sales["sales.gross"] ?: 0.0,
            netSales = sales["sales.net"] ?: 0.0,
            totalTax = sales["sales.tax"] ?: 0.0,
            invoiceCount = (sales["sales.count"] ?: 0.0).toInt(),
            collectionsReceived = collections["collections.collected"] ?: 0.0,
            trend = trend,
        )
    }

    private companion object {
        val SYNCED_ENTITIES = listOf(
            SyncEntity.INVOICE,
            SyncEntity.INVENTORY,
            SyncEntity.PAYMENT_VOUCHER,
            SyncEntity.DEMAND_FORECAST,
        )
    }
}
