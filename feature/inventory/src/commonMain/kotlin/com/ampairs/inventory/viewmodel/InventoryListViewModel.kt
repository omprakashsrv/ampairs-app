package com.ampairs.inventory.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.ampairs.customer.domain.Customer
import com.ampairs.product.domain.Constants.Companion.PAGE_SIZE
import com.ampairs.inventory.db.InventoryRepository
import com.ampairs.inventory.domain.asDomainModel
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.map

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class InventoryListViewModel(val inventoryRepository: InventoryRepository) : ViewModel() {

    var searchText by mutableStateOf("")
    var company: Customer? = null

    val inventories = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        inventoryRepository.getInventoryPaging(searchText)
    }).flow.map { pagingData -> pagingData.map { it.asDomainModel() } }
        .cachedIn(viewModelScope)
}