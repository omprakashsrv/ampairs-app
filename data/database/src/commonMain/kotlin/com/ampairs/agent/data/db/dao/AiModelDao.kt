package com.ampairs.agent.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.ampairs.agent.data.db.entity.AiModelEntity

/**
 * DAO for the persisted on-device model catalog. The catalog is small and read in full, so each
 * successful manifest pull rewrites the table via [replaceAll] — exact and simple, no per-row diffing.
 */
@Dao
interface AiModelDao {

    @Query("SELECT * FROM ai_model_catalog")
    suspend fun getAll(): List<AiModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<AiModelEntity>)

    @Query("DELETE FROM ai_model_catalog")
    suspend fun clear()

    /** Replace the whole persisted catalog with [models] (the latest manifest). */
    @Transaction
    suspend fun replaceAll(models: List<AiModelEntity>) {
        clear()
        insertAll(models)
    }
}
