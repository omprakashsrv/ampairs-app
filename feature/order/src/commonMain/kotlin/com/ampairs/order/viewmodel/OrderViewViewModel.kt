package com.ampairs.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.di.AppScope
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.domain.Order
import kotlinx.coroutines.Dispatchers
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.launch

@AssistedInject
class OrderViewViewModel(@Assisted val orderId: String, val orderRepository: OrderRepository) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(orderId: String): OrderViewViewModel
    }

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