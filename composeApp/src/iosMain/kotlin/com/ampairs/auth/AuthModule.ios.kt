package com.ampairs.auth

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.auth.db.AuthRoomDatabase
import com.ampairs.auth.firebase.FirebaseAuthProvider
import com.ampairs.auth.service.RecaptchaConfig
import com.ampairs.auth.service.RecaptchaService
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import org.koin.core.module.Module
import org.koin.dsl.module

val authPlatformModule: Module = module {
    // Auth database is NOT workspace-aware - login happens before workspace selection
    single<AuthRoomDatabase> {
        Room.databaseBuilder<AuthRoomDatabase>(
            name = getIosDatabasePath("auth.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single {
        RecaptchaService(
            RecaptchaConfig(
                siteKey = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI", // Test key
                enabled = false // Disabled for development
            )
        )
    }

    // Firebase authentication provider
    single { FirebaseAuthProvider() }

    // TODO: Add iOS-specific certificate pinning and security components when needed
    // For now, providing minimal implementation to get compilation working
}