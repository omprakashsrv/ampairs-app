package com.ampairs.unit.di

import com.ampairs.common.di.AppScope
import com.ampairs.unit.data.db.UnitDatabase
import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Replaced Koin unitModule. Injectable classes are annotated with @Inject directly:
// - UnitApiImpl: @Inject @SingleIn(AppScope) @ContributesBinding
// - UnitRepository, UnitStore: @Inject
// - UnitListViewModel, UnitFormViewModel: @Inject
// Platform DB providers are in UnitModule.android/ios/desktop.kt

@ContributesTo(AppScope::class)
interface UnitDaoModule {
    companion object {
        @Provides
        fun provideUnitDao(db: UnitDatabase): UnitDao = db.unitDao()

        @Provides
        fun provideUnitConversionDao(db: UnitDatabase): UnitConversionDao = db.unitConversionDao()
    }
}

fun unitModule() = Unit
