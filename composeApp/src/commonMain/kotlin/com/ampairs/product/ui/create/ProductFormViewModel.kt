package com.ampairs.product.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductImage
import com.ampairs.product.domain.ProductStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ProductFormViewModel(
    private val productId: String?,
    private val productStore: ProductStore,
    private val workspaceContextManager: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    fun loadProduct() {
        if (productId == null) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val key = com.ampairs.product.domain.ProductKey(productId)
                productStore.productStore
                    .stream(org.mobilenativefoundation.store.store5.StoreReadRequest.cached(key, refresh = false))
                    .collect { response ->
                        when (response) {
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Data -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    formState = response.value.toFormState()
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Exception -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = response.error.message ?: "Failed to load product"
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Message -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = response.message
                                )
                            }
                            else -> {
                                // Handle loading state
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load product"
                )
            }
        }
    }

    fun updateForm(newFormState: ProductFormState) {
        val validatedFormState = validateForm(newFormState)
        _uiState.value = _uiState.value.copy(
            formState = validatedFormState,
            canSave = validatedFormState.isValid,
            error = null
        )
    }

    fun addImage() {
        // In a real implementation, this would open image picker
        // For now, we'll add a placeholder
        val currentImages = _uiState.value.formState.images.toMutableList()
        currentImages.add(
            ProductImage(
                productId = productId ?: "",
                image = com.ampairs.product.domain.Image(
                    id = "img_${Clock.System.now().toEpochMilliseconds()}",
                    name = "Product Image ${currentImages.size + 1}",
                    url = null
                )
            )
        )
        updateForm(_uiState.value.formState.copy(images = currentImages))
    }

    fun removeImage(index: Int) {
        val currentImages = _uiState.value.formState.images.toMutableList()
        if (index in currentImages.indices) {
            currentImages.removeAt(index)
            updateForm(_uiState.value.formState.copy(images = currentImages))
        }
    }

    fun saveProduct(onSuccess: () -> Unit) {
        val formState = _uiState.value.formState
        if (!formState.isValid) return

        _uiState.value = _uiState.value.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                val product = formState.toProduct()

                val result = if (productId == null) {
                    productStore.createProduct(product)
                } else {
                    productStore.updateProduct(product.copy(id = productId))
                }

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save product"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save product"
                )
            }
        }
    }

    private fun validateForm(formState: ProductFormState): ProductFormState {
        val nameError = when {
            formState.name.isBlank() -> "Product name is required"
            formState.name.length < 2 -> "Product name must be at least 2 characters"
            formState.name.length > 100 -> "Product name must be less than 100 characters"
            else -> null
        }

        val codeError = when {
            formState.code.isBlank() -> "Product code is required"
            formState.code.length < 2 -> "Product code must be at least 2 characters"
            formState.code.length > 50 -> "Product code must be less than 50 characters"
            !formState.code.matches(Regex("^[a-zA-Z0-9_-]+$")) -> "Product code can only contain letters, numbers, hyphens, and underscores"
            else -> null
        }

        val priceError = when {
            formState.sellingPrice < 0 -> "Selling price cannot be negative"
            formState.mrp < 0 -> "MRP cannot be negative"
            formState.dp < 0 -> "Dealer price cannot be negative"
            formState.mrp > 0 && formState.sellingPrice > formState.mrp -> "Selling price cannot be higher than MRP"
            else -> null
        }

        val stockError = when {
            formState.stockQuantity != null && formState.stockQuantity < 0 -> "Stock quantity cannot be negative"
            formState.lowStockAlert != null && formState.lowStockAlert < 0 -> "Low stock alert cannot be negative"
            formState.stockQuantity != null && formState.lowStockAlert != null &&
                formState.lowStockAlert > formState.stockQuantity -> "Low stock alert cannot be higher than current stock"
            else -> null
        }

        return formState.copy(
            nameError = nameError,
            codeError = codeError,
            priceError = priceError,
            stockError = stockError
        )
    }
}

data class ProductFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val error: String? = null,
    val formState: ProductFormState = ProductFormState()
)