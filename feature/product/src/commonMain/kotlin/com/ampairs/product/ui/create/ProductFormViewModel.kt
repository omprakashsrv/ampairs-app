package com.ampairs.product.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductImage
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.tax.data.repository.TaxCodeLookup
import com.ampairs.tax.domain.model.TaxCode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.ampairs.common.di.AppScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@AssistedInject
@OptIn(FlowPreview::class)
class ProductFormViewModel(
    @Assisted private val productId: String?,
    private val productRepository: ProductRepository,
    private val workspaceContextManager: WorkspaceContextManager,
    private val taxCodeRepository: TaxCodeLookup,
    private val syncService: CentralSyncService,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(productId: String?): ProductFormViewModel
    }

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    private val _taxCodeSearchQuery = MutableStateFlow("")
    val taxCodeSearchQuery: StateFlow<String> = _taxCodeSearchQuery.asStateFlow()

    private val _taxCodeSuggestions = MutableStateFlow<List<TaxCode>>(emptyList())
    val taxCodeSuggestions: StateFlow<List<TaxCode>> = _taxCodeSuggestions.asStateFlow()

    init {
        // Setup tax code search with debounce
        viewModelScope.launch {
            _taxCodeSearchQuery
                .debounce(300) // Wait 300ms after user stops typing
                .collect { query ->
                    searchTaxCodes(query)
                }
        }
    }

    fun loadProduct() {
        if (productId == null) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        productRepository.observeProduct(productId)
            .onEach { product ->
                if (product != null) {
                    val formState = product.toFormState()
                    val taxCodeDescription = if (product.taxCode.isNotBlank()) {
                        try {
                            val taxCodes = taxCodeRepository.searchWorkspaceTaxCodes(product.taxCode, limit = 1)
                            taxCodes.firstOrNull()?.let { "${it.code} - ${it.shortDescription}" }
                        } catch (_: Exception) { null }
                    } else null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        formState = formState.copy(taxCodeDescription = taxCodeDescription)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Product not found")
                }
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load product")
            }
            .launchIn(viewModelScope)
    }

    fun updateForm(newFormState: ProductFormState) {
        val validatedFormState = validateForm(newFormState)
        _uiState.value = _uiState.value.copy(
            formState = validatedFormState,
            canSave = validatedFormState.isValid,
            error = null
        )
    }

    @OptIn(ExperimentalTime::class)
    fun addImage() {
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
                    productRepository.createProduct(product)
                } else {
                    productRepository.updateProduct(product.copy(id = productId))
                }

                if (result.isSuccess) {
                    syncService.markPendingPush(SyncEntity.PRODUCT)
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

    // Tax Code Search Methods
    fun onTaxCodeSearchQueryChange(query: String) {
        _taxCodeSearchQuery.value = query
    }

    private suspend fun searchTaxCodes(query: String) {
        if (query.isBlank()) {
            _taxCodeSuggestions.value = emptyList()
            return
        }

        try {
            val results = taxCodeRepository.searchWorkspaceTaxCodes(query, limit = 10)
            _taxCodeSuggestions.value = results
        } catch (e: Exception) {
            // Graceful failure - keep existing suggestions
        }
    }

    fun selectTaxCode(taxCode: TaxCode) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(
                taxCode = taxCode.code,
                taxCodeDescription = "${taxCode.code} - ${taxCode.shortDescription}"
            )
        )
        _taxCodeSearchQuery.value = ""
        _taxCodeSuggestions.value = emptyList()

        // Increment usage count
        viewModelScope.launch {
            taxCodeRepository.incrementUsageCount(taxCode.id)
        }
    }

    fun clearTaxCode() {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(
                taxCode = "",
                taxCodeDescription = null
            )
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