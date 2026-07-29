@file:OptIn(ExperimentalTime::class)

package com.ampairs.analytics.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.analytics.data.query.DashboardReadFacade
import com.ampairs.analytics.domain.DashboardData
import com.ampairs.analytics.domain.DashboardPeriod
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class DashboardUiState(
    val period: DashboardPeriod = DashboardPeriod.THIS_MONTH,
    val data: DashboardData = DashboardData(),
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
                facade.load(range, today, tz)
            }.onSuccess { data ->
                _uiState.update { it.copy(data = data, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
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
