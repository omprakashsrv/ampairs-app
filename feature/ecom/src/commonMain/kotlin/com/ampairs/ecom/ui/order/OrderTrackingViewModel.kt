package com.ampairs.ecom.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.BuyerInvoiceSummary
import com.ampairs.ecom.api.model.DeliveryAddress
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
import com.ampairs.ecom.data.db.entity.EcomOrderLineItemEntity
import com.ampairs.ecom.data.repository.BuyerInvoiceRepository
import com.ampairs.ecom.data.repository.EcomOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import com.ampairs.ecom.domain.EcomLogger
import com.ampairs.ecom.domain.EcomSession
import kotlinx.serialization.json.Json
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
    val deliveryAddress: DeliveryAddress? = null,
    /** Spec 029 — finalized invoices raised for this order (order↔invoice link); empty if none yet. */
    val invoices: List<BuyerInvoiceSummary> = emptyList(),
)

private val deliveryAddressJson = Json { ignoreUnknownKeys = true }

private fun EcomOrderEntity.parseDeliveryAddress(): DeliveryAddress? =
    delivery_address?.let { raw ->
        runCatching { deliveryAddressJson.decodeFromString(DeliveryAddress.serializer(), raw) }
            .onFailure { EcomLogger.w("Order", "Failed to parse delivery address for $ecom_order_ref", it) }
            .getOrNull()
    }

@AssistedInject
class OrderTrackingViewModel(
    @Assisted val orderRef: String,
    private val orderRepository: EcomOrderRepository,
    private val invoiceRepository: BuyerInvoiceRepository,
    private val session: EcomSession,
) : ViewModel() {

    // Invoices raised for this order (order↔invoice link, spec 029). Fetched live; empty when the
    // buyer isn't linked or none exist yet — never blocks order tracking.
    private val invoices = MutableStateFlow<List<BuyerInvoiceSummary>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<OrderTrackingUiState> = orderRepository.observeOrder(orderRef).flatMapLatest { order ->
        if (order == null) flowOf(OrderTrackingUiState())
        else combine(
            orderRepository.observeLineItems(order.uid),
            invoices,
        ) { items, orderInvoices ->
            OrderTrackingUiState(
                order = order,
                lineItems = items,
                deliveryAddress = order.parseDeliveryAddress(),
                invoices = orderInvoices,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderTrackingUiState())

    init {
        refresh()
    }

    fun refresh() {
        session.activeSlug?.let { slug ->
            viewModelScope.launch { orderRepository.refreshOrder(slug, orderRef) }
        }
        viewModelScope.launch {
            invoiceRepository.getOrderInvoices(orderRef).onSuccess { invoices.value = it }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(orderRef: String): OrderTrackingViewModel
    }
}
