package com.ampairs.storefront.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.ampairs.ecom.data.db.dao.StorefrontDirectoryDao
import com.ampairs.ecom.data.db.entity.StorefrontDirectoryEntity

/**
 * Standalone AppScope database for the storefront-directory offline cache (`storefront_directory.db`).
 *
 * Kept OUT of [StorefrontAppDatabase] on purpose: that file holds durable auth sessions, and coupling
 * a disposable cache to it made the auth store version-fragile (bumping its version crashed older,
 * v1 code with a Room downgrade error). This is a pure, re-pullable cache — it can be recreated
 * destructively on any schema change without data loss, and its version is independent of auth.
 *
 * KMP module (android + iOS), so `@ConstructedBy` + an `expect` [RoomDatabaseConstructor] wires the
 * platform-generated impl (Room KSP runs per-target).
 */
@Database(
    entities = [
        StorefrontDirectoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(StorefrontDirectoryDatabaseConstructor::class)
abstract class StorefrontDirectoryDatabase : RoomDatabase() {
    abstract fun storefrontDirectoryDao(): StorefrontDirectoryDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object StorefrontDirectoryDatabaseConstructor : RoomDatabaseConstructor<StorefrontDirectoryDatabase> {
    override fun initialize(): StorefrontDirectoryDatabase
}
