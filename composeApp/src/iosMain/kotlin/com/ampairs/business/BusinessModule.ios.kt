package com.ampairs.business

import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import org.koin.core.module.Module
import org.koin.dsl.module

val businessPlatformModule: Module = module {
    factory<BusinessDatabase> {
        get<WorkspaceAwareDatabaseFactory>().createDatabase(
            klass = BusinessDatabase::class,
            moduleName = "business"
        )
    }
}
