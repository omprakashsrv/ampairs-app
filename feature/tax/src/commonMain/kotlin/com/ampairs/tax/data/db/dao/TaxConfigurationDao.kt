package com.ampairs.tax.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ampairs.tax.data.db.entity.TaxConfigurationEntity
import kotlinx.coroutines.flow.Flow

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
    suspend fun updateSyncTime(timestamp: Long)

    @Query("DELETE FROM tax_configuration")
    suspend fun delete()
}
