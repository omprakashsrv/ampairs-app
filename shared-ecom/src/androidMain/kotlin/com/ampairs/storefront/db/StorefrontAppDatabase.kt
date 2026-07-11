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
