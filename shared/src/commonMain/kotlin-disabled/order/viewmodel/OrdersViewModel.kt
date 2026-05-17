package com.ampairs.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.customer.viewmodel.PAGE_SIZE
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.db.dto.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class OrdersViewModel(val orderRepository: OrderRepository) : ViewModel() {

    var searchText by mutableStateOf("")
    val ordersState = mutableStateOf<UiState<Boolean>>(UiState.Empty)

    init {
        syncTaxInfos()
    }

    private fun syncTaxInfos() {
        viewModelScope.launch(DispatcherProvider.io) {
            orderRepository.getOrderResource().collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            ordersState.value = UiState.Loading(false)
                        }

                        is Resource.Status.Success -> {
                            ordersState.value = UiState.Success(true)
                        }

                        is Resource.Status.EmptySuccess -> {
                            ordersState.value = UiState.Empty
                        }

                        is Resource.Status.Error -> {
                            val status = response.status
                            ordersState.value = UiState.Error(status.errorMessage)
                        }
                    }
                }
            }
        }
    }

    val orders = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        orderRepository.getOrders(searchText)
    }).flow.map { pagingData -> pagingData.map { it.asDomainModel() } }
        .cachedIn(viewModelScope)
}