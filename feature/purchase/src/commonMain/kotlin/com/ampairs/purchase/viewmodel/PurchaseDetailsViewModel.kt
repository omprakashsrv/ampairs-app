package com.ampairs.purchase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.purchase.db.PurchaseRepository
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.db.entity.PurchaseItemEntity
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PurchaseDetailsUiState(
    val purchase: PurchaseEntity? = null,
    val items: List<PurchaseItemEntity> = emptyList(),
)

@AssistedInject
class PurchaseDetailsViewModel(
    @Assisted val purchaseId: String,
    private val repository: PurchaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseDetailsUiState())
    val uiState: StateFlow<PurchaseDetailsUiState> = _uiState.asStateFlow()

    init {
        repository.observePurchaseById(purchaseId)
            .onEach { p -> _uiState.value = _uiState.value.copy(purchase = p) }
            .launchIn(viewModelScope)
        repository.observePurchaseItems(purchaseId)
            .onEach { items -> _uiState.value = _uiState.value.copy(items = items) }
            .launchIn(viewModelScope)
    }

    fun deletePurchase(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePurchase(purchaseId)
            onDeleted()
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(purchaseId: String): PurchaseDetailsViewModel
    }
}
