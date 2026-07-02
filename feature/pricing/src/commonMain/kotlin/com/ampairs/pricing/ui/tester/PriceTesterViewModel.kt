package com.ampairs.pricing.ui.tester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.PriceResolutionTrace
import com.ampairs.pricing.domain.model.SalesChannel
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.domain.ProductSummary
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the Price Tester / resolution simulator (the design's hero surface). The user picks a
 * context (channel, product, quantity, optional delivery pincode) and sees the effective price plus
 * the ranked "why this price won" trace, all resolved offline from the local price-list read model.
 */
data class PriceTesterUiState(
    val channel: SalesChannel = SalesChannel.WHOLESALE,
    val query: String = "",
    val searchResults: List<ProductSummary> = emptyList(),
    val selectedProduct: ProductSummary? = null,
    val quantity: Double = 1.0,
    val pincode: String = "",
    val trace: PriceResolutionTrace? = null,
    val explainOpen: Boolean = false,
    val isResolving: Boolean = false,
) {
    /** Line total at the resolved unit price for the chosen quantity. */
    val lineTotal: Double?
        get() = trace?.let { it.resolution.effectiveUnitPrice * quantity }
}

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PriceTesterViewModel(
    private val repository: PriceListRepository,
    private val productDataService: ProductDataService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PriceTesterUiState())
    val uiState: StateFlow<PriceTesterUiState> = _uiState.asStateFlow()

    private var resolveJob: Job? = null

    init {
        search("")
    }

    fun setChannel(channel: SalesChannel) {
        _uiState.update { it.copy(channel = channel) }
        resolve()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        search(query)
    }

    fun selectProduct(product: ProductSummary) {
        _uiState.update { it.copy(selectedProduct = product, searchResults = emptyList(), query = "") }
        resolve()
    }

    fun incrementQuantity() = setQuantity(_uiState.value.quantity + 1)

    fun decrementQuantity() = setQuantity(_uiState.value.quantity - 1)

    fun setQuantity(quantity: Double) {
        val clamped = if (quantity < 1.0) 1.0 else quantity
        _uiState.update { it.copy(quantity = clamped) }
        resolve()
    }

    fun onPincodeChange(pincode: String) {
        _uiState.update { it.copy(pincode = pincode.filter { c -> c.isDigit() }.take(6)) }
        resolve()
    }

    fun toggleExplain() = _uiState.update { it.copy(explainOpen = !it.explainOpen) }

    private fun search(term: String) {
        viewModelScope.launch {
            val results = productDataService.searchSummaries(term, limit = 25)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    private fun resolve() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            _uiState.update { it.copy(isResolving = true) }
            val trace = repository.resolveWithTrace(
                channel = state.channel,
                productId = product.id,
                quantity = state.quantity,
                fallbackUnitPrice = product.sellingPrice,
                variantSku = null,
                pincode = state.pincode.ifBlank { null },
            )
            _uiState.update { it.copy(trace = trace, isResolving = false) }
        }
    }
}
