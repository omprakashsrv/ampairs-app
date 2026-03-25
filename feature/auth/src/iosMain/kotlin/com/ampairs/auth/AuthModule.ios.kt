package com.ampairs.auth

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.ampairs.auth.db.AuthRoomDatabase
import com.ampairs.auth.firebase.FirebaseAuthProvider
import com.ampairs.auth.service.RecaptchaConfig
import com.ampairs.auth.service.RecaptchaService
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.platform.getIosDatabasePath
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Migration from version 2 to 3: Add profile picture columns to userEntity
 */
val AUTH_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_url TEXT")
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_thumbnail_url TEXT")
    }
}

val authPlatformModule: Module = module {
    // Auth database is NOT workspace-aware - login happens before workspace selection
    single<AuthRoomDatabase> {
        Room.databaseBuilder<AuthRoomDatabase>(
            name = getIosDatabasePath("auth.db")
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(DispatcherProvider.io)
            .addMigrations(AUTH_MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true) // Only destroy on version downgrades
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