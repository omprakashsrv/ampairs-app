package com.ampairs.storefront.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.db.entity.UserSessionEntity
import com.ampairs.auth.db.entity.UserTokenEntity

/**
 * Consolidated AppScope database for the storefront apps (clientApp / marketplaceApp) — one file
 * (`storefront_app.db`) for the auth tables that previously lived in `auth.db`
 * ([com.ampairs.storefront.di.StorefrontDatabaseModule] imports the legacy file once on upgrade).
 *
 * Android-only module, so no `@ConstructedBy` — Room resolves the generated impl reflectively.
 * Never add `fallbackToDestructiveMigration` here: it carries durable auth sessions.
 *
 * NOTE: this stays at version 1. It once briefly held the storefront-directory offline cache at
 * version 2, but coupling a disposable cache to the durable auth DB made the auth store
 * version-fragile (a build with older, v1 code crashed on a v2 disk with a downgrade error). The
 * cache moved to its own [StorefrontDirectoryDatabase]; a downgrade Migration(2, 1) that drops the
 * old `storefront_directory` table recovers devices that ran that v2 build, keeping auth intact.
 */
@Database(
    entities = [
        UserEntity::class,
        UserTokenEntity::class,
        UserSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StorefrontAppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userTokenDao(): UserTokenDao
    abstract fun userSessionDao(): UserSessionDao
}
