package com.ampairs.product.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.Product
import com.ampairs.common.di.AppScope
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailsUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)

@AssistedInject
class ProductDetailsViewModel(
    @Assisted private val productId: String,
    private val productRepository: ProductRepository
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(productId: String): ProductDetailsViewModel
    }

    private val _uiState = MutableStateFlow(ProductDetailsUiState())
    val uiState: StateFlow<ProductDetailsUiState> = _uiState.asStateFlow()

    init {
        observeProduct()
    }

    private fun observeProduct() {
        _uiState.update { it.copy(isLoading = true) }
        productRepository.observeProduct(productId)
            .onEach { product ->
                _uiState.update { it.copy(product = product, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load product") }
            }
            .launchIn(viewModelScope)
    }

    fun deleteProduct(onSuccess: () -> Unit) {
        val currentProduct = _uiState.value.product ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            try {
                val result = productRepository.deleteProduct(currentProduct.id)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(isDeleting = false, error = result.exceptionOrNull()?.message ?: "Delete failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, error = e.message ?: "Delete failed") }
            }
        }
    }
}
