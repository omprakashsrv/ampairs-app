package com.ampairs.customer.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.data.repository.CustomerRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Customer DAOs are provided by the consolidated workspace database module (:data:database for the
// main app). This module keeps only the feature's non-DAO service bindings.
@ContributesTo(WorkspaceScope::class)
interface CustomerServiceModule {
    companion object {
        @Provides
        fun provideCustomerDataService(repo: CustomerRepository): CustomerDataService = repo
    }
}
