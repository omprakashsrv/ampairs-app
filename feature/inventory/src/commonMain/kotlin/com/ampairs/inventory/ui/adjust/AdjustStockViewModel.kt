package com.ampairs.inventory.ui.adjust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.inventory.data.repository.InventoryTransactionRepository
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.inventory.domain.InventorySettingsProvider
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.util.InventoryConstants
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdjustStockUiState(
    val item: InventoryItem? = null,
    val stockIn: Boolean = false,
    val quantity: Double = 1.0,
    val reason: String = "",
    val allowNegative: Boolean = false,
    val manualAllowed: Boolean = true,
    val saving: Boolean = false,
) {
    val onHand: Double get() = item?.currentStock ?: 0.0
    val preview: Double get() = if (stockIn) onHand + quantity else onHand - quantity
    val blocked: Boolean get() = !stockIn && !allowNegative && preview < 0.0
}

@AssistedInject
class AdjustStockViewModel(
    @Assisted val itemId: String,
    private val itemRepository: InventoryItemRepository,
    private val transactionRepository: InventoryTransactionRepository,
    private val settingsProvider: InventorySettingsProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdjustStockUiState())
    val uiState: StateFlow<AdjustStockUiState> = _uiState.asStateFlow()

    private val _done = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val done: SharedFlow<Unit> = _done.asSharedFlow()

    init {
        viewModelScope.launch {
            val item = itemRepository.getItem(itemId)
            val allowNeg = settingsProvider.allowNegativeStock()
            val manual = settingsProvider.allowManualAdjustments()
            _uiState.update { it.copy(item = item, allowNegative = allowNeg, manualAllowed = manual) }
        }
    }

    fun setDirection(stockIn: Boolean) = _uiState.update { it.copy(stockIn = stockIn) }
    fun setQuantity(q: Double) = _uiState.update { it.copy(quantity = q.coerceAtLeast(0.0)) }
    fun increment() = _uiState.update { it.copy(quantity = it.quantity + 1) }
    fun decrement() = _uiState.update { it.copy(quantity = (it.quantity - 1).coerceAtLeast(0.0)) }
    fun setReason(r: String) = _uiState.update { it.copy(reason = r) }

    fun apply() {
        val s = _uiState.value
        if (s.quantity <= 0.0 || s.blocked || !s.manualAllowed) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val movement = InventoryMovement(
                uid = UidGenerator.generateUid(InventoryConstants.TXN_UID_PREFIX),
                transactionType = if (s.stockIn) InventoryConstants.TXN_TYPE_STOCK_IN else InventoryConstants.TXN_TYPE_STOCK_OUT,
                transactionReason = s.reason.ifBlank {
                    if (s.stockIn) InventoryConstants.REASON_PURCHASE else InventoryConstants.REASON_CORRECTION
                },
                inventoryItemId = itemId,
                quantity = s.quantity,
                sourceType = "MANUAL",
            )
            transactionRepository.recordMovement(movement)
            _uiState.update { it.copy(saving = false) }
            _done.tryEmit(Unit)
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(itemId: String): AdjustStockViewModel
    }
}
