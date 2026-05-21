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
import com.ampairs.common.desktop.DataDirectoryManager
import com.ampairs.common.di.AppScope
import com.ampairs.network.security.AppUpdateEnforcer
import com.ampairs.network.security.CertificateManager
import com.ampairs.network.security.CertificatePinningService
import com.ampairs.network.security.CertificateStorage
import com.ampairs.network.security.DesktopAppUpdateEnforcer
import com.ampairs.network.security.DesktopCertificatePinningService
import com.ampairs.network.security.DesktopCertificateStorage
import com.ampairs.network.security.DesktopSecureEngineFactory
import com.ampairs.network.security.SecureKtorClientFactory
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import java.io.File

val AUTH_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_url TEXT")
        connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_thumbnail_url TEXT")
    }
}

@ContributesTo(AppScope::class)
interface AuthDesktopModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideAuthDatabase(): AuthRoomDatabase {
            val dbFile = File(DataDirectoryManager.getDatabaseDir(), "auth.db")
            return Room.databaseBuilder<AuthRoomDatabase>(
                name = dbFile.absolutePath
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
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

        @Provides @SingleIn(AppScope::class)
        fun provideCertificateStorage(): CertificateStorage = DesktopCertificateStorage()

        @Provides @SingleIn(AppScope::class)
        fun provideCertificateManager(storage: CertificateStorage): CertificateManager =
            CertificateManager(storage)

        @Provides @SingleIn(AppScope::class)
        fun provideAppUpdateEnforcer(): AppUpdateEnforcer = DesktopAppUpdateEnforcer()

        @Provides @SingleIn(AppScope::class)
        fun provideCertificatePinningService(
            manager: CertificateManager,
            enforcer: AppUpdateEnforcer
        ): CertificatePinningService {
            val service = DesktopCertificatePinningService(manager)
            service.setAppUpdateEnforcer(enforcer)
            return service
        }

        @Provides @SingleIn(AppScope::class)
        fun provideSecureEngineFactory(
            pinningService: CertificatePinningService
        ): DesktopSecureEngineFactory =
            DesktopSecureEngineFactory(pinningService as DesktopCertificatePinningService)

        @Provides @SingleIn(AppScope::class)
        fun provideSecureKtorClientFactory(
            pinningService: CertificatePinningService,
            enforcer: AppUpdateEnforcer
        ): SecureKtorClientFactory = SecureKtorClientFactory(pinningService, enforcer)
    }
}
