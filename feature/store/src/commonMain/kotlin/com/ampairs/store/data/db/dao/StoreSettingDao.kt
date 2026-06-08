package com.ampairs.store.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.store.data.db.entity.StoreSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreSettingDao {

    @Query("SELECT * FROM store_settings WHERE active = 1 ORDER BY module ASC, setting_key ASC")
    fun observeAll(): Flow<List<StoreSettingEntity>>

    @Query("SELECT * FROM store_settings WHERE id = :id")
    suspend fun getById(id: String): StoreSettingEntity?

    @Query("SELECT * FROM store_settings WHERE module = :module AND setting_key = :key LIMIT 1")
    suspend fun getByModuleKey(module: String, key: String): StoreSettingEntity?

    @Query("SELECT * FROM store_settings WHERE synced = 0")
    suspend fun getUnsynced(): List<StoreSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: StoreSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<StoreSettingEntity>)

    @Query("DELETE FROM store_settings WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM store_settings")
    suspend fun clearAll()
}
