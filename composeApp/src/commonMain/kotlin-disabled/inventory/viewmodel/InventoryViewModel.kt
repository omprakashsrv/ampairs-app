package com.ampairs.inventory.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.customer.domain.Constants
import com.ampairs.inventory.db.InventoryRepository
import com.ampairs.inventory.domain.Inventory
import com.ampairs.inventory.domain.asDatabaseModel
import com.ampairs.inventory.domain.asDomainModel
import com.ampairs.inventory.ui.InventoryState
import com.ampairs.inventory.ui.toDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InventoryViewModel(val id: String?, private val inventoryRepository: InventoryRepository) :
    ViewModel() {

    var loading by mutableStateOf(false)
    var inventory by mutableStateOf(InventoryState(Inventory()))

    init {
        id?.let { loadInventory(it) }
    }

    private fun loadInventory(inventoryId: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            val inventoryEntity = inventoryRepository.getInventory(inventoryId)
            inventory = InventoryState(inventoryEntity?.asDomainModel() ?: Inventory())
        }
    }

    fun reSyncInventory(id: String) {
        loadInventory(id)
    }

    fun updateInventory(): String {
        loading = true
        val inventoryToUpdate = inventory.toDomainModel()
        if (inventoryToUpdate.id.isEmpty()) {
            inventoryToUpdate.id = IdUtils.generateUniqueId(
                Constants.CUSTOMER_PREFIX,
                Constants.ID_LENGTH
            )
        }
        viewModelScope.launch(DispatcherProvider.io) {
            inventoryRepository.updateInventory(inventoryToUpdate.asDatabaseModel())
            viewModelScope.launch(Dispatchers.Main) {
                loading = false
            }
        }
        return inventoryToUpdate.id
    }

}