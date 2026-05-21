package com.ampairs.auth

import com.ampairs.auth.db.AuthRoomDatabase
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface AuthDaoModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideUserDao(db: AuthRoomDatabase): UserDao = db.userDao()

        @Provides @SingleIn(AppScope::class)
        fun provideTokenDao(db: AuthRoomDatabase): UserTokenDao = db.userTokenDao()

        @Provides @SingleIn(AppScope::class)
        fun provideSessionDao(db: AuthRoomDatabase): UserSessionDao = db.userSessionDao()
    }
}

fun authModule() = Unit
