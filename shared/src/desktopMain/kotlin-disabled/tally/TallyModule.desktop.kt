package com.ampairs.tally

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.tally.db.TallyRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val tallyPlatformModule: Module = module {
    single<TallyRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = TallyRoomDatabase::class,
            moduleName = "tally"
        )
    }
}