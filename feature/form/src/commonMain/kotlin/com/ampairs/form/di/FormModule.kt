package com.ampairs.form.di

import com.ampairs.common.di.AppScope
import com.ampairs.form.data.db.EntityAttributeDefinitionDao
import com.ampairs.form.data.db.EntityFieldConfigDao
import com.ampairs.form.data.db.FormDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Replaced Koin formModule and formPlatformModule expect.
// Injectable classes are annotated with @Inject directly:
// - ConfigApiImpl: @Inject @SingleIn(AppScope) @ContributesBinding
// - ConfigRepository: @Inject
// - FormConfigViewModel: @Inject
// Platform DB providers are in FormPlatformModule.android/ios/desktop.kt

@ContributesTo(AppScope::class)
interface FormDaoModule {
    companion object {
        @Provides
        fun provideEntityFieldConfigDao(db: FormDatabase): EntityFieldConfigDao =
            db.entityFieldConfigDao()

        @Provides
        fun provideEntityAttributeDefinitionDao(db: FormDatabase): EntityAttributeDefinitionDao =
            db.entityAttributeDefinitionDao()
    }
}

fun formModule() = Unit
