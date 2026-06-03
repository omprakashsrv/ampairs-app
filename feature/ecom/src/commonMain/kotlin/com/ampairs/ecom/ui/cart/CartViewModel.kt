package com.ampairs.ecom.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.data.db.entity.CartItemEntity
import com.ampairs.ecom.data.repository.CartRepository
import com.ampairs.ecom.domain.EcomSession
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartUiState(
    val items: List<CartItemEntity> = emptyList(),
    val subtotal: Double = 0.0,
    val itemTotalMrp: Double = 0.0,
    val savings: Double = 0.0,
    val isEmpty: Boolean = true,
)

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class CartViewModel(
    private val cartRepository: CartRepository,
    private val session: EcomSession,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CartUiState> = session.active.flatMapLatest { active ->
        if (active == null) flowOf(CartUiState())
        else cartRepository.observeItems(active.storefrontId).map { items ->
            val subtotal = items.sumOf { it.unit_price * it.quantity }
            val mrpTotal = items.sumOf { (it.mrp_at_add ?: it.unit_price) * it.quantity }
            CartUiState(
                items = items,
                subtotal = subtotal,
                itemTotalMrp = mrpTotal,
                savings = (mrpTotal - subtotal).coerceAtLeast(0.0),
                isEmpty = items.isEmpty(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    fun setQuantity(listedProductId: String, quantity: Int) {
        val slug = session.activeSlug ?: return
        val storefrontId = session.activeStorefrontId ?: return
        viewModelScope.launch {
            cartRepository.setItem(slug, storefrontId, listedProductId, quantity)
                .onFailure { _messages.tryEmit(it.message ?: "Couldn't update cart") }
        }
    }

    fun removeItem(itemId: String) {
        val slug = session.activeSlug ?: return
        val storefrontId = session.activeStorefrontId ?: return
        viewModelScope.launch {
            cartRepository.removeItem(slug, storefrontId, itemId)
                .onFailure { _messages.tryEmit(it.message ?: "Couldn't remove item") }
        }
    }
}
