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
import com.ampairs.common.di.AppScope
import com.ampairs.common.platform.getIosDatabasePath
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

val AUTH_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_url TEXT")
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_thumbnail_url TEXT")
    }
}

@ContributesTo(AppScope::class)
interface AuthIosModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideAuthDatabase(): AuthRoomDatabase {
            return Room.databaseBuilder<AuthRoomDatabase>(
                name = getIosDatabasePath("auth.db")
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(DispatcherProvider.io)
                .addMigrations(AUTH_MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        }

        @Provides @SingleIn(AppScope::class)
        fun provideFirebaseAuthProvider(): FirebaseAuthProvider = FirebaseAuthProvider()

        @Provides @SingleIn(AppScope::class)
        fun provideRecaptchaService(): RecaptchaService =
            RecaptchaService(
                RecaptchaConfig(siteKey = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI", enabled = false)
            )

        // TODO: Add iOS-specific certificate pinning and security components when needed
    }
}
