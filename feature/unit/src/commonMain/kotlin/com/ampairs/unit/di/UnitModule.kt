package com.ampairs.unit.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.unit.agent.UnitAgentDao
import com.ampairs.unit.data.db.UnitDatabase
import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.repository.UnitLookup
import com.ampairs.unit.data.repository.UnitOptionsLookup
import com.ampairs.unit.data.repository.UnitRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface UnitDaoModule {
    companion object {
        @Provides
        fun provideUnitDao(db: UnitDatabase): UnitDao = db.unitDao()

        @Provides
        fun provideUnitConversionDao(db: UnitDatabase): UnitConversionDao = db.unitConversionDao()

        @Provides
        fun provideUnitAgentDao(db: UnitDatabase): UnitAgentDao = db.unitAgentDao()

        @Provides
        fun provideUnitLookup(repo: UnitRepository): UnitLookup = repo

        @Provides
        fun provideUnitOptionsLookup(repo: UnitRepository): UnitOptionsLookup = repo
    }
}

fun unitModule() = Unit
