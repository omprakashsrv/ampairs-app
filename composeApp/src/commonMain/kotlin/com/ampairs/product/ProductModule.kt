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
import com.ampairs.product.ui.variant.VariantFormViewModel
import com.ampairs.product.ui.variant.VariantManagementViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module


val productModule: Module = module {
    // DAOs - Use factory for workspace isolation
    factory { get<ProductRoomDatabase>().productDao() }
    factory { get<ProductRoomDatabase>().productVariantDao() }
    factory { get<ProductRoomDatabase>().variantAttributeDao() }

    // Product API implementation - Stateless, can be single
    single<ProductApi> { ProductApiImpl(get(), get()) }

    // Repository - Use factory for workspace isolation
    factory<ProductRepository> { ProductRepository(get(), get(), get(), get()) }

    // Product Store for offline-first pattern - Use factory for workspace isolation
    factory<ProductStore> { ProductStore(get()) }

    // ViewModels for Store5 pattern
    viewModel { ProductsListViewModel(get(), get()) }
    viewModel { (productId: String?) -> ProductFormViewModel(productId, get(), get(), get()) }
    viewModel { (productId: String) -> ProductDetailsViewModel(productId, get(), get()) }

    // Variant ViewModels
    viewModel { (productId: String) -> VariantManagementViewModel(productId, get()) }
    viewModel { (productId: String, variantId: String?) -> VariantFormViewModel(productId, variantId, get()) }
}

fun productModule() = productModule