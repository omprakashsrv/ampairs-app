package com.ampairs.auth.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.db.entity.UserSessionEntity
import com.ampairs.auth.db.entity.UserTokenEntity

@Database(
    entities = [UserEntity::class, UserTokenEntity::class, UserSessionEntity::class],
    version = 2,
    exportSchema = true
)
@ConstructedBy(AuthRoomDatabaseConstructor::class)
abstract class AuthRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userTokenDao(): UserTokenDao
    abstract fun userSessionDao(): UserSessionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AuthRoomDatabaseConstructor : RoomDatabaseConstructor<AuthRoomDatabase>

