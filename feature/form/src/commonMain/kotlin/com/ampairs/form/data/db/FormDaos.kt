package com.ampairs.form.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** NOTE (spec 011): drafted, compile-gate on device. */
@Dao
interface FormSchemaDao {
    @Query("SELECT * FROM form_schema WHERE entityType = :entityType")
    suspend fun getByEntityType(entityType: String): FormSchemaEntity?

    @Query("SELECT * FROM form_schema WHERE dirty = 1")
    suspend fun getDirty(): List<FormSchemaEntity>

    @Upsert
    suspend fun upsert(schema: FormSchemaEntity)

    @Query("UPDATE form_schema SET dirty = :dirty WHERE entityType = :entityType")
    suspend fun setDirty(entityType: String, dirty: Boolean)
}

@Dao
interface FormSectionDao {
    @Query("SELECT * FROM form_section WHERE entityType = :entityType ORDER BY displayOrder ASC")
    fun observeByEntityType(entityType: String): Flow<List<FormSectionEntity>>

    @Query("SELECT * FROM form_section WHERE entityType = :entityType ORDER BY displayOrder ASC")
    suspend fun getByEntityType(entityType: String): List<FormSectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<FormSectionEntity>)

    @Query("DELETE FROM form_section WHERE entityType = :entityType")
    suspend fun deleteByEntityType(entityType: String)
}

@Dao
interface FormFieldDao {
    @Query("SELECT * FROM form_field WHERE entityType = :entityType ORDER BY displayOrder ASC")
    fun observeByEntityType(entityType: String): Flow<List<FormFieldEntity>>

    @Query("SELECT * FROM form_field WHERE entityType = :entityType ORDER BY displayOrder ASC")
    suspend fun getByEntityType(entityType: String): List<FormFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fields: List<FormFieldEntity>)

    @Query("DELETE FROM form_field WHERE entityType = :entityType")
    suspend fun deleteByEntityType(entityType: String)
}
