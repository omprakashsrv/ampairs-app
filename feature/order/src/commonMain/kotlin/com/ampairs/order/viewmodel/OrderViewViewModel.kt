package com.ampairs.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.domain.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrderViewViewModel(val orderId: String, val orderRepository: OrderRepository) : ViewModel() {

    var order by mutableStateOf(Order())
        private set
    var savingOrder by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            order = orderRepository.getOrder(orderId)
        }
    }

    fun saveOrder() {
        savingOrder = true
        viewModelScope.launch(DispatcherProvider.io) {
            order.let { orderRepository.saveOrder(it) }
            order = orderRepository.getOrder(orderId)
            viewModelScope.launch(Dispatchers.Main) {
                savingOrder = false
            }
        }
    }

    fun createInvoice() {
        savingOrder = true
        viewModelScope.launch(DispatcherProvider.io) {
            orderRepository.createInvoice(order)
            order = orderRepository.getOrder(orderId)
            viewModelScope.launch(Dispatchers.Main) {
                savingOrder = false
            }
        }
    }

}