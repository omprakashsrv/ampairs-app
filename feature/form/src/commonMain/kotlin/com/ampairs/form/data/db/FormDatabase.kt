package com.ampairs.form.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * Unified form schema database (spec 011): aggregate header + section + field tables.
 * Fresh schema — replaces the old entity_field_configs / entity_attribute_definitions tables.
 */
@Database(
    entities = [
        FormSchemaEntity::class,
        FormSectionEntity::class,
        FormFieldEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@ConstructedBy(FormDatabaseConstructor::class)
abstract class FormDatabase : RoomDatabase() {
    abstract fun formSchemaDao(): FormSchemaDao
    abstract fun formSectionDao(): FormSectionDao
    abstract fun formFieldDao(): FormFieldDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FormDatabaseConstructor : RoomDatabaseConstructor<FormDatabase> {
    override fun initialize(): FormDatabase
}
