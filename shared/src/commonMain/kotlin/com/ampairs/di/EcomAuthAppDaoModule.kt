package com.ampairs.di

import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.common.di.AppScope
import com.ampairs.database.AmpairsAppDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Provides the auth DAOs from the consolidated [AmpairsAppDatabase], for the main app only. Kept
 * out of `:data:database`'s `AppDatabaseDaoModule` deliberately: the storefront apps
 * (`:shared-ecom`) also depend on `:data:database` (for the storefront `@Database` class
 * definitions) but not on `:shared`, and provide these same DAOs from their own
 * `StorefrontAppDatabase`. Since Metro merges every `@ContributesTo(AppScope::class)` visible on
 * the classpath, these bindings must live only where `:shared-ecom` can't see them.
 */
@ContributesTo(AppScope::class)
interface EcomAuthAppDaoModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideUserDao(db: AmpairsAppDatabase): UserDao = db.userDao()

        @Provides @SingleIn(AppScope::class)
        fun provideTokenDao(db: AmpairsAppDatabase): UserTokenDao = db.userTokenDao()

        @Provides @SingleIn(AppScope::class)
        fun provideSessionDao(db: AmpairsAppDatabase): UserSessionDao = db.userSessionDao()
    }
}
