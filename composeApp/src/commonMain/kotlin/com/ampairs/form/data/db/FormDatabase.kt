package com.ampairs.form.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        EntityFieldConfigEntity::class,
        EntityAttributeDefinitionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(FormDatabaseConstructor::class)
abstract class FormDatabase : RoomDatabase() {
    abstract fun entityFieldConfigDao(): EntityFieldConfigDao
    abstract fun entityAttributeDefinitionDao(): EntityAttributeDefinitionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FormDatabaseConstructor : RoomDatabaseConstructor<FormDatabase>
