package com.ampairs.form.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntityFieldConfigDao {

    @Query("SELECT * FROM entity_field_configs WHERE entityType = :entityType ORDER BY displayOrder ASC")
    fun getFieldConfigsByEntityType(entityType: String): Flow<List<EntityFieldConfigEntity>>

    @Query("SELECT * FROM entity_field_configs WHERE entityType = :entityType AND visible = 1 ORDER BY displayOrder ASC")
    fun getVisibleFieldConfigsByEntityType(entityType: String): Flow<List<EntityFieldConfigEntity>>

    @Query("SELECT * FROM entity_field_configs WHERE uid = :uid")
    suspend fun getFieldConfigById(uid: String): EntityFieldConfigEntity?

    @Query("SELECT * FROM entity_field_configs WHERE entityType = :entityType AND fieldName = :fieldName")
    suspend fun getFieldConfigByName(entityType: String, fieldName: String): EntityFieldConfigEntity?

    @Query("SELECT * FROM entity_field_configs WHERE synced = 0")
    suspend fun getUnsyncedFieldConfigs(): List<EntityFieldConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldConfig(fieldConfig: EntityFieldConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldConfigs(fieldConfigs: List<EntityFieldConfigEntity>)

    @Update
    suspend fun updateFieldConfig(fieldConfig: EntityFieldConfigEntity)

    @Query("DELETE FROM entity_field_configs WHERE uid = :uid")
    suspend fun deleteFieldConfig(uid: String)

    @Query("DELETE FROM entity_field_configs WHERE entityType = :entityType")
    suspend fun deleteFieldConfigsByEntityType(entityType: String)

    @Query("DELETE FROM entity_field_configs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM entity_field_configs WHERE entityType = :entityType")
    suspend fun getFieldConfigsCount(entityType: String): Int
}
