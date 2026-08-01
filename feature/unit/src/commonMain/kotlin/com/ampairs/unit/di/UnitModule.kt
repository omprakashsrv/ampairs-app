package com.ampairs.unit.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.unit.data.repository.UnitConversionSync
import com.ampairs.unit.data.repository.UnitLookup
import com.ampairs.unit.data.repository.UnitOptionsLookup
import com.ampairs.unit.data.repository.UnitRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Unit DAOs are provided by the consolidated workspace database module (:data:database for the
// main app). This module keeps only the feature's non-DAO service bindings.
@ContributesTo(WorkspaceScope::class)
interface UnitServiceModule {
    companion object {
        @Provides
        fun provideUnitLookup(repo: UnitRepository): UnitLookup = repo

        @Provides
        fun provideUnitOptionsLookup(repo: UnitRepository): UnitOptionsLookup = repo

        @Provides
        fun provideUnitConversionSync(repo: UnitRepository): UnitConversionSync = repo
    }
}
