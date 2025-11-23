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
import com.ampairs.network.security.AndroidAppUpdateEnforcer
import com.ampairs.network.security.AndroidCertificateStorage
import com.ampairs.network.security.AndroidCertificatePinningService
import com.ampairs.network.security.AndroidSecureEngineFactory
import com.ampairs.network.security.AppUpdateEnforcer
import com.ampairs.network.security.CertificateManager
import com.ampairs.network.security.CertificatePinningService
import com.ampairs.network.security.CertificateStorage
import com.ampairs.network.security.SecureKtorClientFactory
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
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
        val context = androidContext()
        val dbFile = context.getDatabasePath("auth.db")
        Room.databaseBuilder<AuthRoomDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(AUTH_MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true) // Only destroy on version downgrades
            .enableMultiInstanceInvalidation() // Support multi-process scenarios
            .build()
    }
    
    single {
        RecaptchaService(
            androidContext(),
            RecaptchaConfig(
                siteKey = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI", // Test key
                enabled = false // Disabled for development
            )
        )
    }

    // Firebase authentication provider
    single { FirebaseAuthProvider() }

    // Certificate pinning components
    single<CertificateStorage> { 
        AndroidCertificateStorage(androidContext()) 
    }
    
    single { 
        CertificateManager(get()) 
    }
    
    single<AppUpdateEnforcer> { 
        AndroidAppUpdateEnforcer(androidContext()) 
    }
    
    single<CertificatePinningService> { 
        val service = AndroidCertificatePinningService(androidContext(), get())
        service.setAppUpdateEnforcer(get())
        service
    }
    
    single { 
        AndroidSecureEngineFactory(get<CertificatePinningService>() as AndroidCertificatePinningService) 
    }
    
    single { 
        SecureKtorClientFactory(get(), get()) 
    }

}