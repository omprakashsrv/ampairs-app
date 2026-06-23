package com.ampairs.inventory.ui.count

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.data.repository.InventoryTransactionRepository
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.inventory.util.InventoryConstants
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountRow(
    val item: InventoryItem,
    val countedText: String,
) {
    val system: Double get() = item.currentStock
    val counted: Double get() = countedText.toDoubleOrNull() ?: system
    val variance: Double get() = counted - system
    val hasVariance: Boolean get() = variance != 0.0
}

data class PhysicalCountUiState(
    val rows: List<CountRow> = emptyList(),
    val reviewMode: Boolean = false,
    val posting: Boolean = false,
    val isLoading: Boolean = true,
) {
    val varianceRows: List<CountRow> get() = rows.filter { it.hasVariance }
    val visibleRows: List<CountRow> get() = if (reviewMode) varianceRows else rows
}

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PhysicalCountViewModel(
    private val itemRepository: InventoryItemRepository,
    private val transactionRepository: InventoryTransactionRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhysicalCountUiState())
    val uiState: StateFlow<PhysicalCountUiState> = _uiState.asStateFlow()

    private val _posted = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val posted: SharedFlow<Int> = _posted.asSharedFlow()

    private val counted = mutableMapOf<String, String>()

    init {
        itemRepository.observeItems()
            .onEach { items ->
                _uiState.update { st ->
                    st.copy(
                        rows = items.map { CountRow(it, counted[it.uid] ?: "") },
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY))
    }

    fun setCount(itemId: String, text: String) {
        counted[itemId] = text
        _uiState.update { st ->
            st.copy(rows = st.rows.map { if (it.item.uid == itemId) it.copy(countedText = text) else it })
        }
    }

    fun toggleReview(review: Boolean) = _uiState.update { it.copy(reviewMode = review) }

    fun confirm() {
        viewModelScope.launch {
            _uiState.update { it.copy(posting = true) }
            val variances = _uiState.value.varianceRows
            variances.forEach { row ->
                transactionRepository.recordMovement(
                    InventoryMovement(
                        uid = UidGenerator.generateUid(InventoryConstants.TXN_UID_PREFIX),
                        transactionType = InventoryConstants.TXN_TYPE_COUNT,
                        transactionReason = InventoryConstants.REASON_COUNT_ADJUSTMENT,
                        inventoryItemId = row.item.uid,
                        // COUNT carries the signed variance directly (see InventoryTransactionRepository).
                        quantity = row.variance,
                        sourceType = "MANUAL",
                    )
                )
            }
            counted.clear()
            _uiState.update { it.copy(posting = false, reviewMode = false) }
            _posted.tryEmit(variances.size)
        }
    }
}
