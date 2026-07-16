package com.ampairs.pricing.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.pricing.data.db.entity.GeoZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoZoneDao {

    @Query("SELECT * FROM geo_zones WHERE active = 1 ORDER BY name ASC")
    fun getAllGeoZones(): Flow<List<GeoZoneEntity>>

    @Query("SELECT * FROM geo_zones WHERE id = :id")
    suspend fun getGeoZoneById(id: String): GeoZoneEntity?

    @Query("SELECT * FROM geo_zones WHERE active = 1")
    suspend fun getActiveGeoZones(): List<GeoZoneEntity>

    @Query("SELECT * FROM geo_zones WHERE synced = 0")
    suspend fun getUnsyncedGeoZones(): List<GeoZoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeoZone(geoZone: GeoZoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeoZones(geoZones: List<GeoZoneEntity>)

    @Query("DELETE FROM geo_zones WHERE id = :id")
    suspend fun hardDeleteGeoZone(id: String)

    @Query("DELETE FROM geo_zones")
    suspend fun clearAll()
}
