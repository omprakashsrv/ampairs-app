package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.ampairs.common.model.UiState
import com.ampairs.invoice.db.dto.asDomainModel
import com.ampairs.invoice.domain.InvoiceStatus
import kotlinx.coroutines.flow.map
import com.ampairs.product.domain.Constants.Companion.PAGE_SIZE
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Single-select list filter (docs/design/order-list-and-view, mirrored for invoices): status
 * chips, plus the two cross-cutting views — converted-from-order rows and not-yet-pushed rows.
 */
enum class InvoiceListFilter(val status: InvoiceStatus? = null) {
    ALL,
    DRAFT(InvoiceStatus.DRAFT),
    NEW(InvoiceStatus.NEW),
    INVOICED(InvoiceStatus.INVOICEED),
    FROM_ORDER,
    OFFLINE,
}

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class InvoicesViewModel(
    val invoiceRepository: InvoiceRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    var searchText by mutableStateOf("")
    var filter by mutableStateOf(InvoiceListFilter.ALL)
    val invoicesState = mutableStateOf<UiState<Boolean>>(UiState.Empty)

    init {
        syncService.observeEntity(SyncEntity.INVOICE)
            .onEach { state ->
                val status = state?.status
                invoicesState.value = when (status) {
                    is SyncStatus.Syncing -> UiState.Loading(false)
                    is SyncStatus.Failed -> UiState.Error(status.reason)
                    else -> UiState.Success(true)
                }
            }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVOICE))
    }

    fun retrySync() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.INVOICE))
    }

    // The paging source factory reads the current search/filter on each invalidation —
    // the screen calls invoices.refresh() after changing either.
    val invoices = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        invoiceRepository.getInvoicesFiltered(
            searchText = searchText,
            status = filter.status?.name ?: "",
            fromOrderOnly = filter == InvoiceListFilter.FROM_ORDER,
            unsyncedOnly = filter == InvoiceListFilter.OFFLINE,
        )
    }).flow.map { pagingData -> pagingData.map { it.asDomainModel() } }
        .cachedIn(viewModelScope)
}
