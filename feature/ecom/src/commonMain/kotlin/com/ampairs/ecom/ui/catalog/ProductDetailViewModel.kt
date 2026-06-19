package com.ampairs.ecom.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.data.db.entity.ListedProductEntity
import com.ampairs.ecom.data.repository.CartRepository
import com.ampairs.ecom.data.repository.CatalogRepository
import com.ampairs.ecom.domain.EcomSession
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: ListedProductEntity? = null,
    val qtyInCart: Int = 0,
    val cartCount: Int = 0,
    val cartTotal: Double = 0.0,
)

@AssistedInject
class ProductDetailViewModel(
    @Assisted val productId: String,
    private val catalogRepository: CatalogRepository,
    private val cartRepository: CartRepository,
    private val session: EcomSession,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProductDetailUiState> = session.active.flatMapLatest { active ->
        if (active == null) {
            flowOf(ProductDetailUiState())
        } else {
            combine(
                catalogRepository.observeProduct(productId),
                cartRepository.observeItems(active.storefrontId),
            ) { product, cartItems ->
                ProductDetailUiState(
                    product = product,
                    qtyInCart = cartItems.firstOrNull { it.listed_product_id == productId }?.quantity ?: 0,
                    cartCount = cartItems.sumOf { it.quantity },
                    cartTotal = cartItems.sumOf { it.unit_price * it.quantity },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    init {
        val slug = session.activeSlug
        val storefrontId = session.activeStorefrontId
        if (slug != null && storefrontId != null) {
            viewModelScope.launch { catalogRepository.refreshProduct(slug, productId, storefrontId) }
        }
    }

    fun setQuantity(quantity: Int) {
        val storefrontId = session.activeStorefrontId ?: return
        viewModelScope.launch {
            cartRepository.setItem(storefrontId, productId, quantity)
                .onFailure { _messages.tryEmit(it.message ?: "Couldn't update cart") }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(productId: String): ProductDetailViewModel
    }
}
