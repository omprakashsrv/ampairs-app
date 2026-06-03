package com.ampairs.file.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.file.db.dao.FileDao
import com.ampairs.file.db.entity.FileEntity

@Database(
    entities = [FileEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(FileRoomDatabaseConstructor::class)
abstract class FileRoomDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FileRoomDatabaseConstructor : RoomDatabaseConstructor<FileRoomDatabase> {
    override fun initialize(): FileRoomDatabase
}
