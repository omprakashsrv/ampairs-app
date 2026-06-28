package com.ampairs.purchase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.purchase.db.PurchaseRepository
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class PurchaseListUiState(
    val purchases: List<PurchaseEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PurchasesListViewModel(
    private val repository: PurchaseRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    /** Reactive purchase list straight from Room (source of truth). */
    val purchases: StateFlow<List<PurchaseEntity>> =
        repository.getAllPurchases()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncState = MutableStateFlow(PurchaseListUiState())
    val syncState: StateFlow<PurchaseListUiState> = _syncState.asStateFlow()

    init {
        // Drive isRefreshing/error from the central sync state, not coroutine lifecycle.
        syncService.observeEntity(SyncEntity.PURCHASE)
            .onEach { state ->
                val status = state?.status
                _syncState.value = PurchaseListUiState(
                    isRefreshing = status is SyncStatus.Syncing,
                    error = (status as? SyncStatus.Failed)?.reason,
                )
            }
            .launchIn(viewModelScope)
        // Initial pull on screen open.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.PURCHASE))
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.PURCHASE))
    }
}
