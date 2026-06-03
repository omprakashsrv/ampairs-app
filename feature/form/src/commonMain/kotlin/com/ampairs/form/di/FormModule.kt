package com.ampairs.form.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.db.EntityAttributeDefinitionDao
import com.ampairs.form.data.db.EntityFieldConfigDao
import com.ampairs.form.data.db.FormDatabase
import com.ampairs.form.data.repository.ConfigLookup
import com.ampairs.form.data.repository.ConfigRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface FormDaoModule {
    companion object {
        @Provides
        fun provideConfigLookup(repo: ConfigRepository): ConfigLookup = repo

        @Provides
        fun provideEntityFieldConfigDao(db: FormDatabase): EntityFieldConfigDao =
            db.entityFieldConfigDao()

        @Provides
        fun provideEntityAttributeDefinitionDao(db: FormDatabase): EntityAttributeDefinitionDao =
            db.entityAttributeDefinitionDao()
    }
}

fun formModule() = Unit
