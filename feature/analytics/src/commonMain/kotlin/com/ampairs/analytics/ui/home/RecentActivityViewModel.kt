package com.ampairs.analytics.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.invoice.agent.InvoiceAgentDao
import com.ampairs.order.agent.OrderAgentDao
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
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

/** Which document a [RecentActivityItem] represents (drives the row icon + tap target). */
enum class RecentActivityType { INVOICE, ORDER }

/** One row of the home "recent activity" feed: a recent invoice or order. */
data class RecentActivityItem(
    val type: RecentActivityType,
    val id: String,
    val number: String,
    val status: String,
    /** Business date, `yyyy-MM-dd` (the date part of the document's local timestamp). */
    val date: String,
)

data class RecentActivityUiState(
    val items: List<RecentActivityItem> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Backs the home screen's "recent activity" feed — the latest invoices and orders interleaved
 * newest-first, read offline from the per-module agent DAOs (no new sync plumbing). Re-reads when an
 * invoice or order sync completes. Dates are compared as `yyyy-MM-dd` strings (lexical == chronological).
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class RecentActivityViewModel(
    private val invoiceAgentDao: InvoiceAgentDao,
    private val orderAgentDao: OrderAgentDao,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentActivityUiState())
    val uiState: StateFlow<RecentActivityUiState> = _uiState.asStateFlow()

    init {
        merge(
            syncService.observeEntity(SyncEntity.INVOICE).map { it?.status is SyncStatus.Success },
            syncService.observeEntity(SyncEntity.ORDER).map { it?.status is SyncStatus.Success },
        )
            .distinctUntilChanged()
            .onEach { done -> if (done) reload() }
            .launchIn(viewModelScope)
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            runCatching {
                val invoices = invoiceAgentDao.recentActivity(PER_TYPE).map {
                    RecentActivityItem(RecentActivityType.INVOICE, it.id, it.number, it.status, it.docDate)
                }
                val orders = orderAgentDao.recentActivity(PER_TYPE).map {
                    RecentActivityItem(RecentActivityType.ORDER, it.id, it.number, it.status, it.docDate)
                }
                (invoices + orders).sortedByDescending { it.date }.take(MAX_ITEMS)
            }.onSuccess { items -> _uiState.value = RecentActivityUiState(items, isLoading = false) }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    private companion object {
        const val PER_TYPE = 6
        const val MAX_ITEMS = 8
    }
}
