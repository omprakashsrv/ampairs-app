@file:OptIn(ExperimentalTime::class)

package com.ampairs.analytics.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.analytics.data.query.DashboardReadFacade
import com.ampairs.common.agent.DateRange
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
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Compact overview shown on the app home (hybrid-home overview strip, feature 022). */
data class HomeSummaryUiState(
    val todaySales: Double = 0.0,
    val todayInvoices: Int = 0,
    val outstanding: Double = 0.0,
    /** Trailing [WEEK_DAYS]-day daily gross-sales series (zero-filled) for the sparkline. */
    val weekSales: List<Double> = emptyList(),
    val isLoading: Boolean = true,
) {
    companion object {
        const val WEEK_DAYS = 7
    }
}

/**
 * Lightweight ViewModel for the home overview strip. Reuses [DashboardReadFacade] to read the same
 * offline-first per-module aggregates as the full dashboard, but only the few figures the home needs:
 * today's sales + invoice count, a snapshot of outstanding receivables, and a trailing 7-day sales
 * sparkline. It never fetches deep history or hits the network directly — it just freshens the local
 * mirrors and re-reads when a sync finishes. Period boundaries resolve in the workspace **business**
 * time zone, which the screen pushes in via [setLocale] (defaults to UTC until then).
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class HomeSummaryViewModel(
    private val facade: DashboardReadFacade,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeSummaryUiState())
    val uiState: StateFlow<HomeSummaryUiState> = _uiState.asStateFlow()

    private var timeZoneId: String = "UTC"

    init {
        // Re-read local aggregates whenever an invoice or payment sync completes.
        merge(
            syncService.observeEntity(SyncEntity.INVOICE).map { it?.status is SyncStatus.Success },
            syncService.observeEntity(SyncEntity.PAYMENT_VOUCHER).map { it?.status is SyncStatus.Success },
        )
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

    private fun reload() {
        viewModelScope.launch {
            runCatching {
                val tz = runCatching { TimeZone.of(timeZoneId) }.getOrDefault(TimeZone.UTC)
                val now = Clock.System.now()
                val today = now.toLocalDateTime(tz).date
                val tomorrowStart = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)

                val todayRange = DateRange(today.atStartOfDayIn(tz), tomorrowStart)
                val weekStart = today.minus(HomeSummaryUiState.WEEK_DAYS - 1, DateTimeUnit.DAY)
                val weekRange = DateRange(weekStart.atStartOfDayIn(tz), tomorrowStart)

                val todayData = facade.load(todayRange, today, tz)
                val weekData = facade.load(weekRange, today, tz)

                // Zero-fill the trend into a dense 7-point series ending today.
                val byDate = weekData.trend.associate { it.bucket to it.total }
                val series = (0 until HomeSummaryUiState.WEEK_DAYS).map { i ->
                    byDate[weekStart.plus(i, DateTimeUnit.DAY).toString()] ?: 0.0
                }

                HomeSummaryUiState(
                    todaySales = todayData.kpis.grossSales,
                    todayInvoices = todayData.kpis.invoiceCount,
                    outstanding = weekData.kpis.outstandingReceivable,
                    weekSales = series,
                    isLoading = false,
                )
            }.onSuccess { state -> _uiState.value = state }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    private companion object {
        val SYNCED_ENTITIES = listOf(SyncEntity.INVOICE, SyncEntity.PAYMENT_VOUCHER)
    }
}
