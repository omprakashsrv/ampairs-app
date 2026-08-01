package com.ampairs.supplier.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.supplier.domain.Supplier
import com.ampairs.supplier.domain.SupplierStore
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupplierDetailsUiState(
    val supplier: Supplier? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Custom attribute rows ready for display (raw key → value). */
    val attributeRows: List<Pair<String, String>> = emptyList(),
)

@AssistedInject
class SupplierDetailsViewModel(
    @Assisted private val supplierId: String,
    private val supplierStore: SupplierStore,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(supplierId: String): SupplierDetailsViewModel
    }

    private val _uiState = MutableStateFlow(SupplierDetailsUiState())
    val uiState: StateFlow<SupplierDetailsUiState> = _uiState.asStateFlow()

    fun loadSupplier() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                supplierStore.observeSupplier(supplierId)
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(isLoading = false, error = throwable.message ?: "Unknown error")
                        }
                    }
                    .collect { supplier ->
                        _uiState.update {
                            it.copy(
                                supplier = supplier,
                                isLoading = false,
                                error = if (supplier == null) "Supplier not found" else null,
                                attributeRows = supplier?.attributes?.toList().orEmpty(),
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load supplier")
                }
            }
        }
    }

    fun deleteSupplier(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = supplierStore.deleteSupplier(supplierId)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to delete supplier"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to delete supplier")
                }
            }
        }
    }
}
