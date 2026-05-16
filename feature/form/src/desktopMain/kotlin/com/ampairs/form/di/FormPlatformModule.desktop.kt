package com.ampairs.form.di

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.form.data.db.FormDatabase
import org.koin.dsl.module

actual val formPlatformModule = module {
    // Use factory to ensure fresh database instances after workspace switch
    factory<FormDatabase> {
        get<WorkspaceAwareDatabaseFactory>().createDatabase(
            klass = FormDatabase::class,
            moduleName = "form"
        )
    }
}
