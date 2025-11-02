package com.ampairs.product

import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.data.api.ProductApiImpl
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.ProductRoomDatabase
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.ProductStore
import com.ampairs.product.ui.create.ProductFormViewModel
import com.ampairs.product.ui.details.ProductDetailsViewModel
import com.ampairs.product.ui.list.ProductsListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module


val productModule: Module = module {
    // Store5 based components
    single<ProductDao> { get<ProductRoomDatabase>().productDao() }

    // Product API implementation
    single<ProductApi> { ProductApiImpl(get(), get()) }

    single<ProductRepository> { ProductRepository(get(), get()) }

    // Product Store for offline-first pattern
    single<ProductStore> { ProductStore(get()) }

    // ViewModels for Store5 pattern
    viewModel { ProductsListViewModel(get(), get()) }
    viewModel { (productId: String?) -> ProductFormViewModel(productId, get(), get()) }
    viewModel { (productId: String) -> ProductDetailsViewModel(productId, get(), get()) }
}

fun productModule() = productModule