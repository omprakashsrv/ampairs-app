package com.ampairs.form.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntityAttributeDefinitionDao {

    @Query("SELECT * FROM entity_attribute_definitions WHERE entityType = :entityType ORDER BY displayOrder ASC")
    fun getAttributeDefinitionsByEntityType(entityType: String): Flow<List<EntityAttributeDefinitionEntity>>

    @Query("SELECT * FROM entity_attribute_definitions WHERE entityType = :entityType AND visible = 1 ORDER BY displayOrder ASC")
    fun getVisibleAttributeDefinitionsByEntityType(entityType: String): Flow<List<EntityAttributeDefinitionEntity>>

    @Query("SELECT * FROM entity_attribute_definitions WHERE entityType = :entityType AND category = :category ORDER BY displayOrder ASC")
    fun getAttributeDefinitionsByCategory(entityType: String, category: String): Flow<List<EntityAttributeDefinitionEntity>>

    @Query("SELECT * FROM entity_attribute_definitions WHERE uid = :uid")
    suspend fun getAttributeDefinitionById(uid: String): EntityAttributeDefinitionEntity?

    @Query("SELECT * FROM entity_attribute_definitions WHERE entityType = :entityType AND attributeKey = :attributeKey")
    suspend fun getAttributeDefinitionByKey(entityType: String, attributeKey: String): EntityAttributeDefinitionEntity?

    @Query("SELECT * FROM entity_attribute_definitions WHERE synced = 0")
    suspend fun getUnsyncedAttributeDefinitions(): List<EntityAttributeDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributeDefinition(attributeDefinition: EntityAttributeDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributeDefinitions(attributeDefinitions: List<EntityAttributeDefinitionEntity>)

    @Update
    suspend fun updateAttributeDefinition(attributeDefinition: EntityAttributeDefinitionEntity)

    @Query("DELETE FROM entity_attribute_definitions WHERE uid = :uid")
    suspend fun deleteAttributeDefinition(uid: String)

    @Query("DELETE FROM entity_attribute_definitions WHERE entityType = :entityType")
    suspend fun deleteAttributeDefinitionsByEntityType(entityType: String)

    @Query("DELETE FROM entity_attribute_definitions")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM entity_attribute_definitions WHERE entityType = :entityType")
    suspend fun getAttributeDefinitionsCount(entityType: String): Int
}
