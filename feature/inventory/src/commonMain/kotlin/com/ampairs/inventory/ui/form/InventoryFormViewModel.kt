package com.ampairs.inventory.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.data.repository.InventoryTransactionRepository
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.domain.InventoryMovement
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

data class InventoryFormUiState(
    val isEdit: Boolean = false,
    val loaded: Boolean = false,
    val name: String = "",
    val sku: String = "",
    val mrp: String = "",
    val sellingPrice: String = "",
    val costPrice: String = "",
    val reorder: String = "",
    val openingStock: String = "",
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

@AssistedInject
class InventoryFormViewModel(
    @Assisted val itemId: String?,
    private val itemRepository: InventoryItemRepository,
    private val transactionRepository: InventoryTransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryFormUiState(isEdit = itemId != null))
    val uiState: StateFlow<InventoryFormUiState> = _uiState.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    private var existing: InventoryItem? = null

    init {
        if (itemId != null) load(itemId) else _uiState.update { it.copy(loaded = true) }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val item = itemRepository.getItem(id)
            existing = item
            if (item != null) {
                _uiState.update {
                    it.copy(
                        loaded = true,
                        name = item.name,
                        sku = item.sku.orEmpty(),
                        mrp = numToText(item.mrp),
                        sellingPrice = numToText(item.sellingPrice),
                        costPrice = numToText(item.costPrice),
                        reorder = numToText(item.reorderLevel),
                    )
                }
            } else {
                _uiState.update { it.copy(loaded = true, error = "Item not found") }
            }
        }
    }

    fun onName(v: String) = _uiState.update { it.copy(name = v, nameError = false) }
    fun onSku(v: String) = _uiState.update { it.copy(sku = v) }
    fun onMrp(v: String) = _uiState.update { it.copy(mrp = v) }
    fun onSellingPrice(v: String) = _uiState.update { it.copy(sellingPrice = v) }
    fun onCostPrice(v: String) = _uiState.update { it.copy(costPrice = v) }
    fun onReorder(v: String) = _uiState.update { it.copy(reorder = v) }
    fun onOpeningStock(v: String) = _uiState.update { it.copy(openingStock = v) }

    fun save() {
        val s = _uiState.value
        if (s.name.isBlank()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val base = existing
            val opening = s.openingStock.toDoubleOrNull() ?: 0.0
            val item = (base ?: InventoryItem()).copy(
                uid = base?.uid?.takeIf { it.isNotBlank() }
                    ?: UidGenerator.generateUid(InventoryConstants.ITEM_UID_PREFIX),
                name = s.name.trim(),
                sku = s.sku.trim().ifBlank { null },
                mrp = s.mrp.toDoubleOrNull() ?: 0.0,
                sellingPrice = s.sellingPrice.toDoubleOrNull() ?: 0.0,
                costPrice = s.costPrice.toDoubleOrNull() ?: 0.0,
                reorderLevel = s.reorder.toDoubleOrNull() ?: 0.0,
                // Create at zero on-hand; the OPENING movement below moves it to `opening` (and the
                // repo applies that delta optimistically) so on-hand is never set silently.
                currentStock = base?.currentStock ?: 0.0,
                availableStock = base?.availableStock ?: 0.0,
                active = true,
            )
            val result = itemRepository.saveItem(item)
            if (result.isFailure) {
                _uiState.update { it.copy(saving = false, error = result.exceptionOrNull()?.message) }
                return@launch
            }
            // Opening stock becomes the item's first OPENING movement — never a silent number.
            if (base == null && opening > 0.0) {
                transactionRepository.recordMovement(
                    InventoryMovement(
                        uid = UidGenerator.generateUid(InventoryConstants.TXN_UID_PREFIX),
                        transactionType = InventoryConstants.TXN_TYPE_STOCK_IN,
                        transactionReason = InventoryConstants.REASON_OPENING,
                        inventoryItemId = item.uid,
                        quantity = opening,
                        sourceType = "MANUAL",
                    )
                )
            }
            _uiState.update { it.copy(saving = false) }
            _saved.tryEmit(Unit)
        }
    }

    private fun numToText(v: Double): String =
        if (v == 0.0) "" else if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(itemId: String?): InventoryFormViewModel
    }
}
