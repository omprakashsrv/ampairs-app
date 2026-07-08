package com.ampairs.file.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
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
