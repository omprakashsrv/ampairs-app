package com.ampairs.product.ui.variant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.ProductVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.launch

/**
 * ViewModel for managing product variants
 */
@AssistedInject
class VariantManagementViewModel(
    @Assisted private val productId: String,
    private val productRepository: ProductRepository
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(productId: String): VariantManagementViewModel
    }

    private val _uiState = MutableStateFlow(VariantManagementUiState())
    val uiState: StateFlow<VariantManagementUiState> = _uiState.asStateFlow()

    init {
        loadVariants()
    }

    private fun loadVariants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Observe variants reactively from database
                productRepository.observeProductVariants(productId).collect { variants ->
                    val totalStock = productRepository.getTotalVariantStock(productId)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            variants = variants,
                            totalStock = totalStock,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load variants"
                    )
                }
            }
        }
    }

    fun showDeleteDialog(variant: ProductVariant) {
        _uiState.update { it.copy(variantToDelete = variant) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(variantToDelete = null) }
    }

    fun deleteVariant() {
        val variant = _uiState.value.variantToDelete ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            val result = productRepository.deleteVariant(variant.id)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            variantToDelete = null,
                            successMessage = "Deleted ${variant.variantName}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            variantToDelete = null,
                            errorMessage = error.message ?: "Failed to delete variant"
                        )
                    }
                }
            )
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI State for Variant Management
 */
data class VariantManagementUiState(
    val isLoading: Boolean = false,
    val variants: List<ProductVariant> = emptyList(),
    val totalStock: Double = 0.0,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val variantToDelete: ProductVariant? = null,
    val isDeleting: Boolean = false
)
