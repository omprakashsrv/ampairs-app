package com.ampairs.storefront.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.db.entity.UserSessionEntity
import com.ampairs.auth.db.entity.UserTokenEntity
import com.ampairs.ecom.data.db.dao.StorefrontDirectoryDao
import com.ampairs.ecom.data.db.entity.StorefrontDirectoryEntity

/**
 * Consolidated AppScope database for the storefront apps (clientApp / marketplaceApp) — one file
 * (`storefront_app.db`) for the auth tables that previously lived in `auth.db`
 * ([com.ampairs.storefront.di.StorefrontDatabaseModule] imports the legacy file once on upgrade)
 * plus the offline-directory cache (`storefront_directory`).
 *
 * Android-only module, so no `@ConstructedBy` — Room resolves the generated impl reflectively.
 * Never add `fallbackToDestructiveMigration` here: it carries durable auth sessions.
 *
 * Schema changes: bump [Database.version] and add a manual migration in
 * `StorefrontAppDatabaseModule` (never destructive — auth data is durable). The
 * `storefront_directory` table is a re-pullable cache, but auth in the same file means a real
 * migration is still required.
 */
@Database(
    entities = [
        UserEntity::class,
        UserTokenEntity::class,
        UserSessionEntity::class,
        StorefrontDirectoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class StorefrontAppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userTokenDao(): UserTokenDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun storefrontDirectoryDao(): StorefrontDirectoryDao
}
