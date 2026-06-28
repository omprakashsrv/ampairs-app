package com.ampairs.communication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommTemplateDao {
    @Query("SELECT * FROM comm_templates WHERE active = 1 ORDER BY name")
    fun observeAllActive(): Flow<List<CommTemplateEntity>>

    @Query("SELECT * FROM comm_templates WHERE uid = :uid")
    suspend fun getByUid(uid: String): CommTemplateEntity?

    @Query("SELECT * FROM comm_templates WHERE code = :code COLLATE NOCASE LIMIT 1")
    suspend fun getByCode(code: String): CommTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommTemplateEntity)

    @Query("SELECT * FROM comm_templates WHERE synced = 0")
    suspend fun unsynced(): List<CommTemplateEntity>

    @Query("UPDATE comm_templates SET synced = 1, base_version = :baseVersion WHERE uid = :uid")
    suspend fun markSynced(uid: String, baseVersion: Long)

    @Query("DELETE FROM comm_templates WHERE uid = :uid")
    suspend fun hardDelete(uid: String)
}

@Dao
interface CommBindingDao {
    @Query("SELECT * FROM comm_bindings WHERE active = 1 ORDER BY event_type")
    fun observeAllActive(): Flow<List<CommBindingEntity>>

    @Query("SELECT * FROM comm_bindings WHERE uid = :uid")
    suspend fun getByUid(uid: String): CommBindingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommBindingEntity)

    @Query("SELECT * FROM comm_bindings WHERE synced = 0")
    suspend fun unsynced(): List<CommBindingEntity>

    @Query("UPDATE comm_bindings SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("DELETE FROM comm_bindings WHERE uid = :uid")
    suspend fun hardDelete(uid: String)
}

@Dao
interface CommScheduleDao {
    @Query("SELECT * FROM comm_schedules WHERE active = 1 ORDER BY name")
    fun observeAllActive(): Flow<List<CommScheduleEntity>>

    @Query("SELECT * FROM comm_schedules WHERE uid = :uid")
    suspend fun getByUid(uid: String): CommScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommScheduleEntity)

    @Query("SELECT * FROM comm_schedules WHERE synced = 0")
    suspend fun unsynced(): List<CommScheduleEntity>

    @Query("UPDATE comm_schedules SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("DELETE FROM comm_schedules WHERE uid = :uid")
    suspend fun hardDelete(uid: String)
}

@Dao
interface CommCampaignDao {
    @Query("SELECT * FROM comm_campaigns WHERE active = 1 ORDER BY name")
    fun observeAllActive(): Flow<List<CommCampaignEntity>>

    @Query("SELECT * FROM comm_campaigns WHERE uid = :uid")
    suspend fun getByUid(uid: String): CommCampaignEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommCampaignEntity)

    @Query("SELECT * FROM comm_campaigns WHERE synced = 0")
    suspend fun unsynced(): List<CommCampaignEntity>

    @Query("UPDATE comm_campaigns SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("DELETE FROM comm_campaigns WHERE uid = :uid")
    suspend fun hardDelete(uid: String)
}

@Dao
interface CommPreferenceDao {
    @Query("SELECT * FROM comm_preferences WHERE active = 1 ORDER BY customer_uid")
    fun observeAllActive(): Flow<List<CommPreferenceEntity>>

    @Query("SELECT * FROM comm_preferences WHERE uid = :uid")
    suspend fun getByUid(uid: String): CommPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommPreferenceEntity)

    @Query("SELECT * FROM comm_preferences WHERE synced = 0")
    suspend fun unsynced(): List<CommPreferenceEntity>

    @Query("UPDATE comm_preferences SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("DELETE FROM comm_preferences WHERE uid = :uid")
    suspend fun hardDelete(uid: String)
}

@Dao
interface CommLogDao {
    @Query("SELECT * FROM comm_logs WHERE active = 1 ORDER BY created_at DESC LIMIT 500")
    fun observeRecent(): Flow<List<CommLogEntity>>

    @Query("SELECT * FROM comm_logs WHERE uid = :uid")
    suspend fun getByUid(uid: String): CommLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommLogEntity)

    @Query("DELETE FROM comm_logs WHERE uid = :uid")
    suspend fun hardDelete(uid: String)
}
