package com.ampairs.cbemployee.di

import com.ampairs.cbemployee.data.repository.EmployeeLookup
import com.ampairs.cbemployee.data.repository.EmployeeRepository
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * cb-employee feature bindings. DAOs are provided by `:data:database`; this module only binds the
 * `-api` lookup interface to the repository.
 */
@ContributesTo(WorkspaceScope::class)
interface CbEmployeeServiceModule {
    companion object {
        @Provides
        fun provideEmployeeLookup(repo: EmployeeRepository): EmployeeLookup = repo
    }
}
