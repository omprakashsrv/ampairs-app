package com.ampairs.customer.di

import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.api.CustomerApiImpl
import com.ampairs.customer.data.api.CustomerGroupApi
import com.ampairs.customer.data.api.CustomerGroupApiImpl
import com.ampairs.customer.data.api.CustomerImageApi
import com.ampairs.customer.data.api.CustomerImageApiImpl
import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.api.CustomerTypeApiImpl
import com.ampairs.customer.data.db.CustomerDatabase
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.data.repository.CustomerImageRepository
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.data.repository.PlatformFileManager
import com.ampairs.customer.data.repository.ImageFilePicker
import com.ampairs.customer.data.repository.FileKitImagePicker
import com.ampairs.customer.domain.CustomerGroupStore
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.domain.CustomerTypeStore
import com.ampairs.customer.domain.StateStore
import com.ampairs.customer.ui.components.images.CustomerImageViewModel
import com.ampairs.customer.ui.create.CustomerFormViewModel
import com.ampairs.customer.ui.customergroup.CustomerGroupFormViewModel
import com.ampairs.customer.ui.customergroup.CustomerGroupListViewModel
import com.ampairs.customer.ui.customertype.CustomerTypeFormViewModel
import com.ampairs.customer.ui.customertype.CustomerTypeListViewModel
import com.ampairs.customer.ui.details.CustomerDetailsViewModel
import com.ampairs.customer.ui.list.CustomersListViewModel
import com.ampairs.customer.ui.state.StateListViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val customerModule = module {
    includes(customerPlatformModule)

    // API Layer
    singleOf(::CustomerApiImpl) bind CustomerApi::class
    singleOf(::CustomerTypeApiImpl) bind CustomerTypeApi::class
    singleOf(::CustomerGroupApiImpl) bind CustomerGroupApi::class
    singleOf(::CustomerImageApiImpl) bind CustomerImageApi::class

    // Database Layer - Use factory to get fresh DAOs with new database instance
    factory { get<CustomerDatabase>().customerDao() }
    factory { get<CustomerDatabase>().customerTypeDao() }
    factory { get<CustomerDatabase>().customerGroupDao() }
    factory { get<CustomerDatabase>().customerImageDao() }
    factory { get<CustomerDatabase>().stateDao() }

    // File Picker
    singleOf(::FileKitImagePicker) bind ImageFilePicker::class

    // Repository Layer - Use factory to recreate with new DAOs after workspace switch
    factory { CustomerRepository(get(), get(), get(), get()) }
    factory { CustomerTypeRepository(get(), get(), get()) }
    factory { CustomerGroupRepository(get(), get(), get()) }
    factory { CustomerImageRepository(get(), get(), get(), get()) }

    // Domain Layer - Use factory to recreate Stores with new repositories/DAOs
    factory { CustomerStore(get()) }
    factory { CustomerTypeStore(get(), get()) }
    factory { CustomerGroupStore(get(), get()) }
    factory { StateStore(get(), get()) }

    // ViewModels
    viewModelOf(::CustomersListViewModel)
    viewModel { (customerId: String?) -> CustomerDetailsViewModel(customerId ?: "", get()) }
    viewModel { (customerId: String?) ->
        CustomerFormViewModel(
            customerId,
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    viewModelOf(::StateListViewModel)

    // CustomerType ViewModels
    viewModelOf(::CustomerTypeListViewModel)
    viewModel { (customerTypeId: String?) -> CustomerTypeFormViewModel(get(), customerTypeId) }

    // CustomerGroup ViewModels
    viewModelOf(::CustomerGroupListViewModel)
    viewModel { (customerGroupId: String?) -> CustomerGroupFormViewModel(get(), customerGroupId) }

    // CustomerImage ViewModels
    viewModel { (customerId: String) -> CustomerImageViewModel(customerId, get(), get()) }
}

expect val customerPlatformModule: org.koin.core.module.Module