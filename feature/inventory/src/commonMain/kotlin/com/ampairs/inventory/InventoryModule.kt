package com.ampairs.inventory

import com.ampairs.agent.core.ActionHandlerProvider
import com.ampairs.inventory.agent.InventoryActionHandler
import com.ampairs.inventory.api.InventoryApi
import com.ampairs.inventory.api.InventoryApiImpl
import com.ampairs.inventory.db.InventoryRepository
import com.ampairs.inventory.db.InventoryRoomDatabase
import com.ampairs.inventory.viewmodel.InventoryListViewModel
import com.ampairs.inventory.viewmodel.InventoryViewModel
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

val inventoryModule: Module = module {
    factory { InventoryApiImpl(get(), get()) } bind (InventoryApi::class)
    // Database is provided by platform-specific modules (factory for workspace-scoped isolation)
    factory { get<InventoryRoomDatabase>().inventoryDao() }
    factory { InventoryRepository(get(), get()) }

    factory<ActionHandlerProvider> {
        ActionHandlerProvider("inventory", InventoryActionHandler.ACTIONS) {
            InventoryActionHandler(get())
        }
    } bind ActionHandlerProvider::class
    
    // Direct ViewModel injection
    factory { InventoryListViewModel(get()) }
    factory { (id: String?) -> InventoryViewModel(id, get()) }
}

fun inventoryModule() = inventoryModule