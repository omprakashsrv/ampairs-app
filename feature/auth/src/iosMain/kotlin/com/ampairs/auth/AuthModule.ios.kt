package com.ampairs.auth

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.ampairs.auth.firebase.FirebaseAuthProvider
import com.ampairs.auth.service.RecaptchaConfig
import com.ampairs.auth.service.RecaptchaService
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

val AUTH_MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_url TEXT")
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_thumbnail_url TEXT")
    }
}

@ContributesTo(AppScope::class)
interface AuthIosModule {
    companion object {
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
