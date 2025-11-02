package com.ampairs.inventory

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
    single { InventoryApiImpl(get(), get()) } bind (InventoryApi::class)
    // Database is provided by platform-specific modules
    single { get<InventoryRoomDatabase>().inventoryDao() }
    single { InventoryRepository(get(), get()) }
    
    // Direct ViewModel injection
    factory { InventoryListViewModel(get()) }
    factory { (id: String?) -> InventoryViewModel(id, get()) }
}

fun inventoryModule() = inventoryModule