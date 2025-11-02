package com.ampairs.business

import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.data.api.BusinessApiImpl
import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.BusinessStore
import com.ampairs.business.ui.BusinessProfileViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val businessModule: Module = module {
    // Use factory scope for workspace-aware components (Constitution Principle III)
    factory<BusinessDao> { get<BusinessDatabase>().businessDao() }

    factory<BusinessApi> { BusinessApiImpl(get(), get()) }

    factory { BusinessRepository(get(), get(), get()) }
    factory { BusinessStore(get()) }

    // ViewModels for business management screens
    viewModel { com.ampairs.business.ui.BusinessOverviewViewModel(get()) }
    viewModel { com.ampairs.business.ui.BusinessProfileViewModel(get()) }
    viewModel { com.ampairs.business.ui.BusinessOperationsViewModel(get()) }
    viewModel { com.ampairs.business.ui.BusinessTaxConfigViewModel(get()) }
    viewModel { com.ampairs.business.ui.BusinessCustomAttributesViewModel(get(), get()) }
}

fun businessModule(): Module = businessModule
