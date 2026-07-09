package com.ampairs.auth.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.db.entity.UserSessionEntity
import com.ampairs.auth.db.entity.UserTokenEntity

@Database(
    entities = [UserEntity::class, UserTokenEntity::class, UserSessionEntity::class],
    version = 3,
    exportSchema = true
)
@ConstructedBy(AuthRoomDatabaseConstructor::class)
abstract class AuthRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userTokenDao(): UserTokenDao
    abstract fun userSessionDao(): UserSessionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AuthRoomDatabaseConstructor : RoomDatabaseConstructor<AuthRoomDatabase> {
    override fun initialize(): AuthRoomDatabase
}

