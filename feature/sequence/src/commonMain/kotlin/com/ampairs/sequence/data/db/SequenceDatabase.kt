package com.ampairs.sequence.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.ampairs.sequence.data.db.dao.SequenceAllocationDao
import com.ampairs.sequence.data.db.dao.SequenceDefinitionDao
import com.ampairs.sequence.data.db.entity.SequenceAllocationEntity
import com.ampairs.sequence.data.db.entity.SequenceDefinitionEntity

/**
 * Sequence module Room database — workspace-isolated via WorkspaceAwareDatabaseFactory.
 *
 * Version: 1 (initial)
 * Entities: SequenceDefinitionEntity, SequenceAllocationEntity
 */
@Database(
    entities = [
        SequenceDefinitionEntity::class,
        SequenceAllocationEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(SequenceDatabaseConstructor::class)
abstract class SequenceDatabase : RoomDatabase() {
    abstract fun sequenceDefinitionDao(): SequenceDefinitionDao
    abstract fun sequenceAllocationDao(): SequenceAllocationDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SequenceDatabaseConstructor : RoomDatabaseConstructor<SequenceDatabase> {
    override fun initialize(): SequenceDatabase
}
