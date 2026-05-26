package com.ampairs.auth

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.ampairs.auth.db.AuthRoomDatabase
import com.ampairs.auth.firebase.FirebaseAuthProvider
import com.ampairs.auth.service.RecaptchaConfig
import com.ampairs.auth.service.RecaptchaService
import com.ampairs.common.di.AppScope
import com.ampairs.network.security.AndroidAppUpdateEnforcer
import com.ampairs.network.security.AndroidCertificatePinningService
import com.ampairs.network.security.AndroidCertificateStorage
import com.ampairs.network.security.AndroidSecureEngineFactory
import com.ampairs.network.security.AppUpdateEnforcer
import com.ampairs.network.security.CertificateManager
import com.ampairs.network.security.CertificatePinningService
import com.ampairs.network.security.CertificateStorage
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

val AUTH_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_url TEXT")
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_thumbnail_url TEXT")
    }
}

@ContributesTo(AppScope::class)
interface AuthAndroidModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideAuthDatabase(context: Context): AuthRoomDatabase {
            val dbFile = context.getDatabasePath("auth.db")
            return Room.databaseBuilder<AuthRoomDatabase>(
                context = context,
                name = dbFile.absolutePath
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(AUTH_MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .enableMultiInstanceInvalidation()
                .build()
        }

        @Provides @SingleIn(AppScope::class)
        fun provideFirebaseAuthProvider(): FirebaseAuthProvider = FirebaseAuthProvider()

        @Provides @SingleIn(AppScope::class)
        fun provideRecaptchaService(context: Context): RecaptchaService =
            RecaptchaService(
                context,
                RecaptchaConfig(siteKey = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI", enabled = false)
            )

        @Provides @SingleIn(AppScope::class)
        fun provideCertificateStorage(context: Context): CertificateStorage =
            AndroidCertificateStorage(context)

        @Provides @SingleIn(AppScope::class)
        fun provideCertificateManager(storage: CertificateStorage): CertificateManager =
            CertificateManager(storage)

        @Provides @SingleIn(AppScope::class)
        fun provideAppUpdateEnforcer(context: Context): AppUpdateEnforcer =
            AndroidAppUpdateEnforcer(context)

        @Provides @SingleIn(AppScope::class)
        fun provideCertificatePinningService(
            context: Context,
            manager: CertificateManager,
            enforcer: AppUpdateEnforcer
        ): CertificatePinningService {
            val service = AndroidCertificatePinningService(context, manager)
            service.setAppUpdateEnforcer(enforcer)
            return service
        }

        @Provides @SingleIn(AppScope::class)
        fun provideSecureEngineFactory(
            pinningService: CertificatePinningService
        ): AndroidSecureEngineFactory =
            AndroidSecureEngineFactory(pinningService as AndroidCertificatePinningService)

//        @Provides @SingleIn(AppScope::class)
//        fun provideSecureKtorClientFactory(
//            pinningService: CertificatePinningService,
//            enforcer: AppUpdateEnforcer
//        ): SecureKtorClientFactory = SecureKtorClientFactory(pinningService, enforcer)
    }
}
