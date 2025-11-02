package com.ampairs.product.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductKey
import com.ampairs.product.domain.ProductStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

data class ProductDetailsUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)

class ProductDetailsViewModel(
    private val productId: String,
    private val productStore: ProductStore,
    private val workspaceContextManager: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailsUiState())
    val uiState: StateFlow<ProductDetailsUiState> = _uiState.asStateFlow()

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val key = ProductKey(productId)
                productStore.productStore
                    .stream(StoreReadRequest.cached(key, refresh = false))
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Unknown error"
                            )
                        }
                    }
                    .collect { response ->
                        when (response) {
                            is StoreReadResponse.Data -> {
                                _uiState.update {
                                    it.copy(
                                        product = response.value,
                                        isLoading = false,
                                        error = null
                                    )
                                }
                            }
                            is StoreReadResponse.Loading -> {
                                _uiState.update { it.copy(isLoading = true) }
                            }
                            is StoreReadResponse.Error.Exception -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = response.error.message ?: "Unknown error"
                                    )
                                }
                            }
                            is StoreReadResponse.Error.Message -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = response.message
                                    )
                                }
                            }
                            else -> {
                                // Handle other response types if needed
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load product"
                    )
                }
            }
        }
    }

    fun refreshProduct() {
        viewModelScope.launch {
            try {
                val key = ProductKey(productId)
                productStore.productStore
                    .stream(StoreReadRequest.fresh(key))
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(error = throwable.message ?: "Refresh failed")
                        }
                    }
                    .collect { response ->
                        when (response) {
                            is StoreReadResponse.Data -> {
                                _uiState.update {
                                    it.copy(
                                        product = response.value,
                                        error = null
                                    )
                                }
                            }
                            is StoreReadResponse.Error.Exception -> {
                                _uiState.update {
                                    it.copy(error = response.error.message ?: "Refresh failed")
                                }
                            }
                            is StoreReadResponse.Error.Message -> {
                                _uiState.update {
                                    it.copy(error = response.message)
                                }
                            }
                            else -> {
                                // Handle other response types if needed
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Refresh failed")
                }
            }
        }
    }

    fun deleteProduct(onSuccess: () -> Unit) {
        val currentProduct = _uiState.value.product ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }

            try {
                val result = productStore.deleteProduct(currentProduct.id)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = result.exceptionOrNull()?.message ?: "Delete failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        error = e.message ?: "Delete failed"
                    )
                }
            }
        }
    }
}