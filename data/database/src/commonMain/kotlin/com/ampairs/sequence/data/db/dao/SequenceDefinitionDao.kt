package com.ampairs.sequence.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.sequence.data.db.entity.SequenceDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SequenceDefinitionDao {

    @Query("SELECT * FROM sequence_definitions WHERE active = 1 ORDER BY entity_type ASC")
    fun observeActiveDefinitions(): Flow<List<SequenceDefinitionEntity>>

    @Query("SELECT * FROM sequence_definitions WHERE uid = :uid")
    suspend fun getByUid(uid: String): SequenceDefinitionEntity?

    /** Active workspace-scoped definition for an entity type (resolution fallback / provisional prefix). */
    @Query(
        "SELECT * FROM sequence_definitions WHERE entity_type = :entityType AND scope = 'WORKSPACE' AND active = 1 LIMIT 1"
    )
    suspend fun getActiveWorkspaceDefinition(entityType: String): SequenceDefinitionEntity?

    @Query("SELECT * FROM sequence_definitions WHERE synced = 0")
    suspend fun getUnsyncedDefinitions(): List<SequenceDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefinition(definition: SequenceDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefinitions(definitions: List<SequenceDefinitionEntity>)

    @Query("DELETE FROM sequence_definitions WHERE uid = :uid")
    suspend fun hardDeleteDefinition(uid: String)
}
