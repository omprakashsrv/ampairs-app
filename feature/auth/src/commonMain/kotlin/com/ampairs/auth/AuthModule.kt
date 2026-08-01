package com.ampairs.auth

import com.ampairs.auth.api.UserDataService
import com.ampairs.auth.db.UserRepository
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// Auth DAOs are provided by the composition root's consolidated app database module
// (:data:database for the main app, shared-ecom's storefront database for the client apps).
// This module keeps only the feature's non-DAO service bindings.
@ContributesTo(AppScope::class)
interface AuthServiceModule {
    companion object {
        @Provides
        fun provideUserDataService(repo: UserRepository): UserDataService = repo
    }
}

fun authModule() = Unit
