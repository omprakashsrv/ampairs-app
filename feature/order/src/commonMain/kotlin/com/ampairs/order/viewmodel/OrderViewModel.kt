package com.ampairs.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.domain.Customer
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.OrderItem
import com.ampairs.order.domain.TaxInfo
import com.ampairs.order.domain.TaxSpec
import com.ampairs.order.domain.asDatabaseModel
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.domain.ProductSummary
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.launch


@AssistedInject
class OrderViewModel(
    @Assisted fromCustomerId: String?, @Assisted toCustomerId: String?, @Assisted id: String?,
    val customerDataService: CustomerDataService,
    val orderRepository: OrderRepository,
    val productDataService: ProductDataService,
    val tokenRepository: TokenRepository,
) :
    ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(fromCustomerId: String?, toCustomerId: String?, id: String?): OrderViewModel
    }
    fun updateOrderItems(products: List<ProductSummary>) {
        orderItems.removeAll(orderItems.filter { orderItem ->
            !products.map { it.id }.contains(orderItem.product?.id)
        })
        products.forEach { product ->
            val item = orderItems.find { orderItem -> orderItem.product?.id == product.id }
            if (item != null) {
                item.quantity = product.quantity
            } else {
                orderItems.add(OrderItem(product))
            }
        }
        orderItems.removeAll(orderItems.filter { orderItem -> orderItem.quantity <= 0 })
        order.items = orderItems
        updateTaxInfos()
    }

    fun saveOrder(onOrderSaved: (String) -> Unit) {
        savingOrder = true
        viewModelScope.launch(DispatcherProvider.io) {
            order.updateTaxes()
            order.updateDiscount()
            val userId = tokenRepository.getCurrentUserId() ?: ""
            if (order.createdBy.isEmpty()) {
                order.createdBy = userId
            }
            order.updatedBy = userId
            val orderEntity = order.asDatabaseModel()
            orderRepository.saveOrder(orderEntity, orderItems.asDatabaseModel(orderEntity.id))
            onOrderSaved(orderEntity.id)
            savingOrder = false
        }
    }

    fun updateTaxInfos() {
        // TODO: Re-implement once productRepository.getTaxCode() is available
    }

    var fromCustomer: Customer? = null
    var toCustomer: Customer? = null
    val orderItems = mutableStateListOf<OrderItem>()
    var selectedOrderItem by mutableStateOf<OrderItem?>(null)
    var savingOrder by mutableStateOf(false)
    var order = Order()

    init {
        viewModelScope.launch(DispatcherProvider.io) {
            if (!id.isNullOrEmpty()) {
                order = orderRepository.getOrder(id)
                fromCustomer = order.fromCustomer
                toCustomer = order.toCustomer
                orderItems.addAll(order.items)
            } else {
                fromCustomer =
                    fromCustomerId?.let { customerDataService.getById(it) }
                toCustomer =
                    toCustomerId?.let { customerDataService.getById(it) }
                order.fromCustomer = fromCustomer
                order.toCustomer = toCustomer
            }
        }
    }
}