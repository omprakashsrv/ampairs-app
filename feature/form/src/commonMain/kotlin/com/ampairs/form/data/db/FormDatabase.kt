package com.ampairs.form.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.ampairs.form.agent.FormAgentDao

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
    abstract fun formAgentDao(): FormAgentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FormDatabaseConstructor : RoomDatabaseConstructor<FormDatabase> {
    override fun initialize(): FormDatabase
}
