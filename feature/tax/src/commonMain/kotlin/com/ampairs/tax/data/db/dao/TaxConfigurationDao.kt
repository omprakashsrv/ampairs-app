package com.ampairs.tax.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.ampairs.tax.data.db.entity.TaxConfigurationEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Tax Configuration DAO
 *
 * Note: No workspace_id in queries - workspace isolation handled by WorkspaceAwareDatabaseFactory
 * There should only be one configuration per workspace database
 */
@Dao
interface TaxConfigurationDao {

    @Query("SELECT * FROM tax_configuration LIMIT 1")
    fun observeConfiguration(): Flow<TaxConfigurationEntity?>

    @Query("SELECT * FROM tax_configuration LIMIT 1")
    suspend fun getConfiguration(): TaxConfigurationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: TaxConfigurationEntity)

    @Update
    suspend fun update(config: TaxConfigurationEntity)

    @Query("UPDATE tax_configuration SET synced_at = :timestamp")
    suspend fun updateSyncTime(timestamp: Instant)

    @Query("DELETE FROM tax_configuration")
    suspend fun delete()
}
