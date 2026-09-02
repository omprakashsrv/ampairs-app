package com.ampairs.cbstore.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.cbstore.data.db.entity.StoreEntity
import com.ampairs.cbstore.data.db.entity.ZonalOfficeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZonalOfficeDao {

    @Query("SELECT * FROM cb_zonal_offices WHERE active = 1 ORDER BY name ASC")
    fun getAllZonalOffices(): Flow<List<ZonalOfficeEntity>>

    @Query("SELECT * FROM cb_zonal_offices WHERE id = :id")
    suspend fun getZonalOfficeById(id: String): ZonalOfficeEntity?

    @Query("SELECT * FROM cb_zonal_offices WHERE synced = 0")
    suspend fun getUnsyncedZonalOffices(): List<ZonalOfficeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZonalOffice(office: ZonalOfficeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZonalOffices(offices: List<ZonalOfficeEntity>)

    @Query("UPDATE cb_zonal_offices SET active = 0, synced = 0 WHERE id = :id")
    suspend fun softDeleteZonalOffice(id: String)

    @Query("DELETE FROM cb_zonal_offices WHERE id = :id")
    suspend fun hardDeleteZonalOffice(id: String)
}

@Dao
interface StoreDao {

    @Query("SELECT * FROM cb_stores WHERE active = 1 ORDER BY code ASC")
    fun getAllStores(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM cb_stores WHERE zonal_office_id = :zoneId AND active = 1 ORDER BY code ASC")
    fun getStoresByZone(zoneId: String): Flow<List<StoreEntity>>

    @Query("SELECT * FROM cb_stores WHERE id = :id")
    suspend fun getStoreById(id: String): StoreEntity?

    @Query("SELECT * FROM cb_stores WHERE synced = 0")
    suspend fun getUnsyncedStores(): List<StoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: StoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<StoreEntity>)

    @Query("UPDATE cb_stores SET active = 0, synced = 0 WHERE id = :id")
    suspend fun softDeleteStore(id: String)

    @Query("DELETE FROM cb_stores WHERE id = :id")
    suspend fun hardDeleteStore(id: String)
}
