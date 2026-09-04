package com.ampairs.cbstore.di

import com.ampairs.cbstore.data.repository.StoreLookup
import com.ampairs.cbstore.data.repository.StoreRepository
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * cb-store feature bindings. DAOs are provided by the consolidated workspace database module
 * (`:data:database`); this module only binds the `-api` lookup interface to the repository.
 */
@ContributesTo(WorkspaceScope::class)
interface CbStoreServiceModule {
    companion object {
        @Provides
        fun provideStoreLookup(repo: StoreRepository): StoreLookup = repo
    }
}
