package com.ampairs.inventory.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.data.repository.InventoryTransactionRepository
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class LedgerUiState(
    val movements: List<InventoryMovement> = emptyList(),
    val itemNames: Map<String, String> = emptyMap(),
    val typeFilter: String? = null,
    val reasonFilter: String? = null,
    val sourceFilter: String? = null,
    val search: String = "",
    val isLoading: Boolean = true,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class MovementLedgerViewModel(
    private val transactionRepository: InventoryTransactionRepository,
    private val itemRepository: InventoryItemRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    private val _type = MutableStateFlow<String?>(null)
    private val _reason = MutableStateFlow<String?>(null)
    private val _source = MutableStateFlow<String?>(null)
    private val _search = MutableStateFlow("")

    init {
        observe()
        itemRepository.observeAllItemsIncludingInactive()
            .onEach { items -> _uiState.update { st -> st.copy(itemNames = items.associate { it.uid to it.name }) } }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY_TRANSACTION))
    }

    private data class Filters(val type: String?, val reason: String?, val source: String?, val search: String)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observe() {
        combine(_type, _reason, _source, _search.debounce(250).distinctUntilChanged()) { type, reason, source, search ->
            Filters(type, reason, source, search)
        }
            .flatMapLatest { f ->
                transactionRepository.observeFilteredMovements(
                    type = f.type,
                    reason = f.reason,
                    sourceType = f.source,
                    reference = f.search.ifBlank { null },
                )
            }
            .onEach { movements -> _uiState.update { it.copy(movements = movements, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun setType(type: String?) { _type.value = type; _uiState.update { it.copy(typeFilter = type) } }
    fun setReason(reason: String?) { _reason.value = reason; _uiState.update { it.copy(reasonFilter = reason) } }
    fun setSource(source: String?) { _source.value = source; _uiState.update { it.copy(sourceFilter = source) } }
    fun setSearch(q: String) { _search.value = q; _uiState.update { it.copy(search = q) } }

    fun clearFilters() {
        _type.value = null; _reason.value = null; _source.value = null; _search.value = ""
        _uiState.update { it.copy(typeFilter = null, reasonFilter = null, sourceFilter = null, search = "") }
    }
}
