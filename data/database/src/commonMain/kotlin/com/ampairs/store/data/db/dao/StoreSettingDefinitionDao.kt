package com.ampairs.store.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.ampairs.store.data.db.entity.StoreSettingDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreSettingDefinitionDao {

    @Query("SELECT * FROM store_setting_definitions ORDER BY module ASC, setting_key ASC")
    fun observeAll(): Flow<List<StoreSettingDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(definitions: List<StoreSettingDefinitionEntity>)

    @Query("DELETE FROM store_setting_definitions")
    suspend fun clearAll()

    /** Replace the whole catalog with the latest server snapshot (definitions are authoritative). */
    @Transaction
    suspend fun replaceAll(definitions: List<StoreSettingDefinitionEntity>) {
        clearAll()
        insertAll(definitions)
    }
}
