package com.ampairs.form.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.repository.ConfigLookup
import com.ampairs.form.data.repository.ConfigRepository
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Form DAOs are provided by the consolidated workspace database module (:data:database for the
// main app). This module keeps only the feature's non-DAO service bindings.
@ContributesTo(WorkspaceScope::class)
interface FormServiceModule {
    companion object {
        @Provides
        fun provideConfigLookup(repo: ConfigRepository): ConfigLookup = repo
    }
}
