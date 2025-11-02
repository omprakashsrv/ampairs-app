package com.ampairs.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.db.UserRepository
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.customer.db.CustomerRepository
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.asDomainModel
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.OrderItem
import com.ampairs.order.domain.TaxInfo
import com.ampairs.order.domain.TaxSpec
import com.ampairs.order.domain.asDatabaseModel
import com.ampairs.product.domain.Product
import com.ampairs.product.ui.product.ProductViewModel
import com.ampairs.repository.ProductRepository
import kotlinx.coroutines.launch


class OrderViewModel(
    fromCustomerId: String?, toCustomerId: String?, id: String?,
    val customerRepository: CustomerRepository,
    val orderRepository: OrderRepository,
    val productRepository: ProductRepository,
    val userRepository: UserRepository,
    productViewModel: ProductViewModel,
) :
    ViewModel() {
    fun updateOrderItems(products: List<Product>) {
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
            if (order.createdBy.isEmpty()) {
                order.createdBy = userRepository.getUser()?.id ?: ""
            }
            order.updatedBy = userRepository.getUser()?.id ?: ""
            val orderEntity = order.asDatabaseModel()
            orderRepository.saveOrder(orderEntity, orderItems.asDatabaseModel(orderEntity.id))
            onOrderSaved(orderEntity.id)
            savingOrder = false
        }
    }

    fun updateTaxInfos() {
        viewModelScope.launch(DispatcherProvider.io) {
            orderItems.forEach { orderItem ->
                orderItem.product?.taxInfos =
                    orderItem.product?.taxCode?.let { productRepository.getTaxCode(it)?.taxInfos }
                orderItem.taxInfos =
                    orderItem.product?.taxInfos?.filter { it.taxSpec.name == order.taxSpec.name }
                        ?.map {
                            TaxInfo(
                                id = it.id,
                                name = it.name,
                                formattedName = it.formattedName,
                                taxSpec = TaxSpec.valueOf(it.taxSpec.name),
                                percentage = it.percentage,
                                value = 0.0
                            )
                        } ?: arrayListOf()
            }
        }

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
                productViewModel.cartProducts =
                    orderItems.map { orderItem -> orderItem.product!! }.toMutableList()
            } else {
                fromCustomer =
                    fromCustomerId?.let { customerRepository.getCustomer(it)?.asDomainModel() }
                toCustomer =
                    toCustomerId?.let { customerRepository.getCustomer(it)?.asDomainModel() }
                order.fromCustomer = fromCustomer
                order.toCustomer = toCustomer
            }
        }
    }
}