package com.ampairs.business

import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface BusinessIosModule {
    companion object {
        @Provides
        fun provideBusinessDatabase(factory: WorkspaceAwareDatabaseFactory): BusinessDatabase =
            factory.createDatabase(klass = BusinessDatabase::class, moduleName = "business")
    }
}
