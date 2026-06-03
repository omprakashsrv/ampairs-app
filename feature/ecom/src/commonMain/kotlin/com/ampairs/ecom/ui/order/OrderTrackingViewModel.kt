package com.ampairs.ecom.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
import com.ampairs.ecom.data.db.entity.EcomOrderLineItemEntity
import com.ampairs.ecom.data.repository.EcomOrderRepository
import com.ampairs.ecom.domain.EcomSession
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrderTrackingUiState(
    val order: EcomOrderEntity? = null,
    val lineItems: List<EcomOrderLineItemEntity> = emptyList(),
)

@AssistedInject
class OrderTrackingViewModel(
    @Assisted val orderRef: String,
    private val orderRepository: EcomOrderRepository,
    private val session: EcomSession,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<OrderTrackingUiState> = orderRepository.observeOrder(orderRef).flatMapLatest { order ->
        if (order == null) flowOf(OrderTrackingUiState())
        else combine(
            flowOf(order),
            orderRepository.observeLineItems(order.uid),
        ) { o, items -> OrderTrackingUiState(order = o, lineItems = items) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderTrackingUiState())

    init {
        session.activeSlug?.let { slug ->
            viewModelScope.launch { orderRepository.refreshOrder(slug, orderRef) }
        }
    }

    fun refresh() {
        session.activeSlug?.let { slug ->
            viewModelScope.launch { orderRepository.refreshOrder(slug, orderRef) }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(orderRef: String): OrderTrackingViewModel
    }
}
