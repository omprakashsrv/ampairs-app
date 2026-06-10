package com.ampairs.form.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.db.FormDatabase
import com.ampairs.form.data.db.FormFieldDao
import com.ampairs.form.data.db.FormSchemaDao
import com.ampairs.form.data.db.FormSectionDao
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
        fun provideFormSchemaDao(db: FormDatabase): FormSchemaDao = db.formSchemaDao()

        @Provides
        fun provideFormSectionDao(db: FormDatabase): FormSectionDao = db.formSectionDao()

        @Provides
        fun provideFormFieldDao(db: FormDatabase): FormFieldDao = db.formFieldDao()
    }
}
